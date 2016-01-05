/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.jetbrains.kotlin.incremental

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File
import java.util.*


fun getJavaSourceRoots(sources: Iterable<File>, roots: Iterable<File>): Iterable<File> =
        sources
            .filter { it.isJavaFile() }
            .map { findSrcDirRoot(it, roots) }
            .filterNotNull()

private fun File.isJavaFile() = extension.equals(JavaFileType.INSTANCE.defaultExtension, ignoreCase = true)


fun makeModuleFile(name: String, isTest: Boolean, outputDir: File, sourcesToCompile: List<File>, javaSourceRoots: Iterable<File>, classpath: Iterable<File>, friendDirs: Iterable<File>): File {
    val builder = KotlinModuleXmlBuilder()
    builder.addModule(
            name,
            outputDir.absolutePath,
            sourcesToCompile,
            javaSourceRoots.map { JvmSourceRoot(it) },
            classpath,
            "java-production",
            isTest,
            // this excludes the output directories from the class path, to be removed for true incremental compilation
            setOf(outputDir),
            friendDirs
    )

    val scriptFile = File.createTempFile("kjps", StringUtil.sanitizeJavaIdentifier(name) + ".script.xml")

    FileUtil.writeToFile(scriptFile, builder.asText().toString())

    return scriptFile
}


private fun findSrcDirRoot(file: File, roots: Iterable<File>): File? {
    for (root in roots) {
        if (FileUtil.isAncestor(root, file, false)) {
            return root
        }
    }
    return null
}

fun createCompileServices(
        incrementalCaches: Map<TargetId, IncrementalCache>,
        lookupTracker: LookupTracker,
        compilationCanceledStatus: CompilationCanceledStatus?
): Services {
    val builder = Services.Builder()
    builder.register(IncrementalCompilationComponents::class.java, BasicIncrementalCompilationComponentsImpl(incrementalCaches, lookupTracker))
    compilationCanceledStatus?.let {
        builder.register(CompilationCanceledStatus::class.java, it)
    }
    return builder.build()
}

fun makeLookupTracker(parentLookupTracker: LookupTracker = LookupTracker.DO_NOTHING): LookupTracker =
        if (IncrementalCompilation.isExperimental()) LookupTrackerImpl(parentLookupTracker)
        else parentLookupTracker


fun<Target> getIncrementalCaches(
        targets: Iterable<Target>,
        getDependencies: (Target) -> Iterable<Target>,
        getCache: (Target) -> BasicIncrementalCacheImpl<Target>,
        getTargetId: Target.() -> TargetId
): Map<TargetId, BasicIncrementalCacheImpl<Target>>
{
    val dependents = targets.keysToMap { hashSetOf<Target>() }
    val targetsWithDependents = targets.toHashSet()

    for (target in targets) {
        for (dependency in getDependencies(target)) {
            if (dependency !in targets) continue

            dependents[dependency]!!.add(target)
            targetsWithDependents.add(target)
        }
    }

    val caches = targetsWithDependents.keysToMap { getCache(it) }

    for ((target, cache) in caches) {
        dependents[target]?.forEach {
            cache.addDependentCache(caches[it]!!)
        }
    }

    return caches.mapKeys { it.key.getTargetId() }
}


fun<Target> updateKotlinIncrementalCache(
        targets: Iterable<Target>,
        compilationErrors: Boolean,
        getIncrementalCache: (Target) -> BasicIncrementalCacheImpl<Target>,
        generatedFiles: List<GeneratedFile<Target>>
): CompilationResult {

    assert(IncrementalCompilation.isEnabled()) { "updateKotlinIncrementalCache should not be called when incremental compilation disabled" }

    var changesInfo = CompilationResult.NO_CHANGES
    for (generatedFile in generatedFiles) {
        val ic = getIncrementalCache(generatedFile.target)
        when {
            generatedFile is GeneratedJvmClass<Target> -> changesInfo += ic.saveFileToCache(generatedFile)
            generatedFile.outputFile.isModuleMappingFile() -> changesInfo += ic.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
        }
    }

    if (!compilationErrors) {
        targets.forEach {
            val newChangesInfo = getIncrementalCache(it).clearCacheForRemovedClasses()
            changesInfo += newChangesInfo
        }
    }

    return changesInfo
}


fun updateLookupStorage(
        lookupStorage: BasicLookupStorage,
        lookupTracker: LookupTracker,
        filesToCompile: Iterable<File>, removedFiles: Iterable<File>
) {
    if (lookupTracker !is LookupTrackerImpl) throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker.javaClass}")

    filesToCompile.forEach { lookupStorage.removeLookupsFrom(it) }
    removedFiles.forEach { lookupStorage.removeLookupsFrom(it) }

    lookupTracker.lookups.entrySet().forEach { lookupStorage.add(it.key, it.value) }
}


fun<Target> getGeneratedFiles(
        targets: Collection<Target>,
        representativeTarget: Target,
        getSources: (Target) -> Iterable<File>,
        getOutputDir: (Target) -> File?,
        outputItemCollector: OutputItemsCollectorImpl
): List<GeneratedFile<Target>> {
    // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
    val sourceToTarget = HashMap<File, Target>()
    if (targets.size > 1) {
        for (target in targets) {
            for (file in getSources(target)) {
                sourceToTarget.put(file, target)
            }
        }
    }

    val result = ArrayList<GeneratedFile<Target>>()

    for (outputItem in outputItemCollector.outputs) {
        val sourceFiles = outputItem.sourceFiles
        val outputFile = outputItem.outputFile
        val target =
                sourceFiles.firstOrNull()?.let { sourceToTarget[it] } ?:
                targets.filter { getOutputDir(it)?.let { outputFile.startsWith(it) } ?: false }.singleOrNull() ?:
                representativeTarget

        if (outputFile.getName().endsWith(".class")) {
            result.add(GeneratedJvmClass(target, sourceFiles, outputFile))
        }
        else {
            result.add(GeneratedFile(target, sourceFiles, outputFile))
        }
    }
    return result
}


fun CompilationResult.dirtyFiles(lookupStorage: BasicLookupStorage): Sequence<File> =
    // TODO group by fqName?
    changes.mapNotNull { it as? ChangeInfo.MembersChanged }
           .flatMap { change ->
               change.names.asSequence()
                       .flatMap { lookupStorage.get(LookupSymbol(it, change.fqName.asString())).asSequence() }
                       .map(::File)
           }


