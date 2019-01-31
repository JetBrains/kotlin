/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildUtils.idea

import IntelliJInstrumentCodeTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import idea.DistCopyDetailsMock
import idea.DistModelBuildContext
import org.codehaus.groovy.runtime.GStringImpl
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.*
import org.gradle.api.internal.file.archive.ZipFileTree
import org.gradle.api.internal.file.collections.*
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.internal.file.copy.DestinationRootCopySpec
import org.gradle.api.internal.file.copy.SingleParentCopySpec
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.file.PathToFileResolver
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.Callable

open class DistModelBuilder(val rootProject: Project, pw: PrintWriter) {
    val rootCtx = DistModelBuildContext(null, "ROOT", "dist", pw)
    val visited = mutableMapOf<Task, DistModelBuildContext>()
    val vfsRoot = DistVFile(null, "<root>", File(""))
    val refs = mutableSetOf<DistVFile>()

    fun visitInstrumentTask(it: IntelliJInstrumentCodeTask): DistModelBuildContext = visited.getOrPut(it) {
        val ctx = rootCtx.child("INSTRUMENT", it.path)
        ctx.setDest(it.output!!.path)
        processSourcePath(it.originalClassesDirs, ctx)
        val dest = ctx.destination
        if (dest != null) {
            DistModuleOutput(dest, it.project.path)
        }

        ctx
    }

    fun visitCompileTask(it: AbstractCompile): DistModelBuildContext = visited.getOrPut(it) {
        val ctx = rootCtx.child("COMPILE", it.path)
        ctx.setDest(it.destinationDir.path)
        val dest = ctx.destination
        if (dest != null) DistModuleOutput(dest, it.project.path)
        else ctx.logUnsupported("Cannot add contents: destination is unknown", it)

        ctx
    }

    fun visitCopyTask(
        copy: AbstractCopyTask,
        shade: Boolean = false
    ): DistModelBuildContext = visited.getOrPut(copy) {
        val context = rootCtx.child("COPY", copy.path, shade)


        val rootSpec = copy.rootSpec

        when (copy) {
            is Copy -> context.setDest(copy.destinationDir.path)
            is Sync -> context.setDest(copy.destinationDir.path)
            is AbstractArchiveTask -> context.setDest(copy.archivePath.path)
        }

        when (copy) {
            is ShadowJar -> copy.configurations.forEach {
                processSourcePath(it, context)
            }
        }

        processCopySpec(rootSpec, context)


        context
    }

    fun processCopySpec(spec: CopySpecInternal, ctx: DistModelBuildContext) {
        spec.children.forEach {
            when (it) {
                is DestinationRootCopySpec -> ctx.child("DESTINATION ROOT COPY SPEC") { newCtx ->
                    newCtx.setDest(getRelativePath(it.destinationDir.path))
                    processCopySpec(it, newCtx)
                }
                is DefaultCopySpec -> ctx.child("DEFAULT COPY SPEC") { newCtx ->
                    val buildRootResolver = it.buildRootResolver()
                    ctx.addCopyActions(buildRootResolver.allCopyActions)
                    newCtx.setDest(buildRootResolver.destPath.getFile(ctx.destination!!.file).path)
                    processCopySpec(it, newCtx)
                    it.includes

                    newCtx.child("SINGE PARENT COPY SPEC") { child ->
                        it.sourcePaths.forEach {
                            processSourcePath(it, child)
                        }
                    }
                }
                is SingleParentCopySpec -> ctx.child("OTHER SINGE PARENT COPY SPEC") { child ->
                    it.sourcePaths.forEach {
                        processSourcePath(it, child)
                    }
                }
                is CopySpecInternal -> processCopySpec(it, ctx)
                else -> ctx.logUnsupported("CopySpec", spec)
            }
        }
    }

    fun processSourcePath(sourcePath: Any?, ctx: DistModelBuildContext) {
        when {
            sourcePath == null -> Unit
            sourcePath is Jar -> ctx.child("JAR") { child ->
                child.addCopyOf(sourcePath.archivePath.path)
            }
            sourcePath is SourceSetOutput -> ctx.child("COMPILE") { child ->
                sourcePath.classesDirs.files.forEach {
                    child.addCopyOf(it.path)
                }
            }
            sourcePath is Configuration -> {
                ctx.child("CONFIGURATION") { child ->
                    sourcePath.resolve().forEach {
                        child.addCopyOf(it.path)
                    }
                }
            }
            sourcePath is SourceDirectorySet -> {
                ctx.child("SOURCES") { child ->
                    sourcePath.srcDirs.forEach {
                        child.addCopyOf(it.path)
                    }
                }
            }
            sourcePath is MinimalFileSet -> ctx.child("MINIMAL FILE SET (${sourcePath.javaClass.simpleName})") { child ->
                sourcePath.files.forEach {
                    processSourcePath(it, child)
                }
            }
            sourcePath is MinimalFileTree -> ctx.child("MINIMAL FILE TREE (${sourcePath.javaClass.simpleName})") { child ->
                sourcePath.visit(object : FileVisitor {
                    override fun visitDir(dirDetails: FileVisitDetails) {
                        processSourcePath(dirDetails.file, child)
                    }

                    override fun visitFile(fileDetails: FileVisitDetails) {
                        processSourcePath(fileDetails.file, child)
                    }
                })
            }
            sourcePath is FileTreeAdapter && sourcePath.tree is GeneratedSingletonFileTree -> ctx.child("FILE TREE ADAPTER OF MAP FILE TREE (${sourcePath.javaClass.simpleName})") { child ->
                sourcePath.visitContents(object : FileCollectionResolveContext {
                    override fun add(element: Any): FileCollectionResolveContext {
                        processSourcePath(element, child)
                        return this
                    }

                    override fun newContext(): ResolvableFileCollectionResolveContext {
                        error("not supported")
                    }

                    override fun push(fileResolver: PathToFileResolver): FileCollectionResolveContext {
                        return this
                    }
                })
            }
            sourcePath is CompositeFileCollection -> ctx.child("COMPOSITE FILE COLLECTION") { child ->
                sourcePath.visitLeafCollections(object : FileCollectionLeafVisitor {
                    override fun visitFileTree(file: File, patternSet: PatternSet) {
                        child.child("FILE TREE") {
                            it.addCopyOf(file.path)
                        }
                    }

                    override fun visitGenericFileTree(fileTree: FileTreeInternal) {
                        child.child("TREE") {
                            processSourcePath(fileTree, it)
                        }
                    }

                    override fun visitCollection(fileCollection: FileCollectionInternal) {
                        processSourcePath(fileCollection, child)
                    }
                })
            }
            sourcePath is FileTreeAdapter && sourcePath.tree is ZipFileTree -> ctx.child("ZIP FILE TREE ADAPTER") { child ->
                val tree = sourcePath.tree
                val field = tree.javaClass.declaredFields.find { it.name == "zipFile" }!!
                field.isAccessible = true
                val zipFile = field.get(tree) as File

                child.addCopyOf(zipFile.path)
            }
            sourcePath is FileTreeInternal -> ctx.child("FILE TREE INTERNAL") { child ->
                // todo: preserve or warn about filtering
                sourcePath.visitTreeOrBackingFile(object : FileVisitor {
                    override fun visitFile(fileDetails: FileVisitDetails) {
                        child.addCopyOf(fileDetails.file.path)
                    }

                    override fun visitDir(dirDetails: FileVisitDetails) {
                        child.addCopyOf(dirDetails.file.path)
                    }
                })
            }
            sourcePath is FileCollection -> ctx.child("OTHER FILE COLLECTION (${sourcePath.javaClass})") { child ->
                try {
                    sourcePath.files.forEach {
                        child.addCopyOf(it.path)
                    }
                } catch (t: Throwable) {
                    child.logUnsupported("FILE COLLECTION (${t.message})", sourcePath)
                }
            }
            sourcePath is String || sourcePath is GStringImpl -> ctx.child("STRING") { child ->
                child.addCopyOf(sourcePath.toString())
            }
            sourcePath is Callable<*> -> ctx.child("CALLABLE") { child ->
                processSourcePath(sourcePath.call(), child)
            }
            sourcePath is Collection<*> -> ctx.child("COLLECTION") { child ->
                sourcePath.forEach {
                    processSourcePath(it, child)
                }
            }
            sourcePath is Copy -> ctx.child("COPY OUTPUT") { child ->
                val src = visitCopyTask(sourcePath).destination
                if (src != null) child.addCopyOf(src)
                // else it is added to `it`, because destination is inhereted by context
            }
            sourcePath is File -> ctx.child("FILE ${sourcePath.path}") { child ->
                child.addCopyOf(sourcePath.path)
            }
            else -> ctx.logUnsupported("SOURCE PATH", sourcePath)
        }
    }

    inline fun DistModelBuildContext.addCopyOf(
        src: String,
        body: (src: DistVFile, target: DistVFile) -> Unit = { _, _ -> Unit }
    ) {
        addCopyOf(requirePath(src), body)
    }

    fun DistModelBuildContext.transformName(srcName: String): String? {
        val detailsMock = DistCopyDetailsMock(this, srcName)
        allCopyActions.forEach {
            detailsMock.lastAction = it
            try {
                it.execute(detailsMock)
            } catch (t: DistCopyDetailsMock.E) {
                // skip
            }
        }
        val name1 = detailsMock.relativePath.lastName
        return if (name1.endsWith(".jar")) transformJarName(name1) else name1
    }

    // todo: investigate why allCopyActions not working
    open fun transformJarName(name: String): String = name

    inline fun DistModelBuildContext.addCopyOf(
        src: DistVFile,
        body: (src: DistVFile, target: DistVFile) -> Unit = { _, _ -> Unit }
    ) {

        val destination = destination
        if (destination != null) {
            body(src, destination)
            val customTargetName = transformName(src.name)
            DistCopy(destination, src, customTargetName)
            log("+DistCopy", "${getRelativePath(src.file.path)} -> ${getRelativePath(destination.file.path)}/$customTargetName")
        } else logUnsupported("Cannot add copy of `$src`: destination is unknown")
    }

    fun DistModelBuildContext.setDest(path: String) {
        destination = vfsRoot.relativePath(path)
        log("INTO", getRelativePath(path))
    }

    fun checkRefs() {
        refs.forEach {
            if (!it.hasContents && it.contents.isEmpty() && it.file.path.contains("${File.pathSeparator}build${File.pathSeparator}")) {
                logger.error("UNRESOLVED ${it.file}")
                it.contents.forEach {
                    logger.error("+ ${it}")
                }
            }
        }
    }

    fun getRelativePath(path: String) = path.replace(rootProject.projectDir.path, "$")

    fun requirePath(targetPath: String): DistVFile {
        val target = vfsRoot.relativePath(targetPath)
        if (!File(targetPath).exists()) refs.add(target)
        return target
    }
}