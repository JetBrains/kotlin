/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.build.GeneratedFile
import org.jetbrains.kotlin.build.GeneratedJvmClass
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.build.isModuleMappingFile
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.synthetic.SAM_LOOKUP_NAME
import java.io.File
import java.util.*

const val DELETE_MODULE_FILE_PROPERTY = "kotlin.delete.module.file.after.build"

fun makeModuleFile(
        name: String,
        isTest: Boolean,
        outputDir: File,
        sourcesToCompile: Iterable<File>,
        commonSources: Iterable<File>,
        javaSourceRoots: Iterable<JvmSourceRoot>,
        classpath: Iterable<File>,
        friendDirs: Iterable<File>
): File {
    val builder = KotlinModuleXmlBuilder()
    builder.addModule(
            name,
            outputDir.absolutePath,
            // important to transform file to absolute paths,
            // otherwise compiler will use module file's parent as base path (a temporary file; see below)
            // (see org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler.getAbsolutePaths)
            sourcesToCompile.map { it.absoluteFile },
            javaSourceRoots,
            classpath,
            commonSources.map { it.absoluteFile },
            null,
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

fun makeCompileServices(
        incrementalCaches: Map<TargetId, IncrementalCache>,
        lookupTracker: LookupTracker,
        compilationCanceledStatus: CompilationCanceledStatus?
): Services =
    with(Services.Builder()) {
        register(LookupTracker::class.java, lookupTracker)
        register(IncrementalCompilationComponents::class.java, IncrementalCompilationComponentsImpl(incrementalCaches))
        compilationCanceledStatus?.let {
            register(CompilationCanceledStatus::class.java, it)
        }
        build()
    }

fun updateIncrementalCache(
    generatedFiles: Iterable<GeneratedFile>,
    cache: IncrementalJvmCache,
    changesCollector: ChangesCollector,
    javaChangesTracker: JavaClassesTrackerImpl?
) {
    for (generatedFile in generatedFiles) {
        when {
            generatedFile is GeneratedJvmClass -> cache.saveFileToCache(generatedFile, changesCollector)
            generatedFile.outputFile.isModuleMappingFile() -> cache.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
        }
    }

    javaChangesTracker?.javaClassesUpdates?.forEach {
        (source, serializedJavaClass) ->
        cache.saveJavaClassProto(source, serializedJavaClass, changesCollector)
    }

    cache.clearCacheForRemovedClasses(changesCollector)
}

fun LookupStorage.update(
        lookupTracker: LookupTracker,
        filesToCompile: Iterable<File>,
        removedFiles: Iterable<File>
) {
    if (lookupTracker !is LookupTrackerImpl) throw AssertionError("Lookup tracker is expected to be LookupTrackerImpl, got ${lookupTracker::class.java}")

    removeLookupsFrom(filesToCompile.asSequence() + removedFiles.asSequence())

    addAll(lookupTracker.lookups.entrySet(), lookupTracker.pathInterner.values)
}

data class DirtyData(
        val dirtyLookupSymbols: Collection<LookupSymbol> = emptyList(),
        val dirtyClassesFqNames: Collection<FqName> = emptyList()
)

fun ChangesCollector.getDirtyData(
    caches: Iterable<IncrementalCacheCommon>,
    reporter: ICReporter
): DirtyData {
    val dirtyLookupSymbols = HashSet<LookupSymbol>()
    val dirtyClassesFqNames = HashSet<FqName>()

    for (change in changes()) {
        reporter.report { "Process $change" }

        if (change is ChangeInfo.SignatureChanged) {
            val fqNames = if (!change.areSubclassesAffected) listOf(change.fqName) else withSubtypes(change.fqName, caches)
            dirtyClassesFqNames.addAll(fqNames)

            for (classFqName in fqNames) {
                assert(!classFqName.isRoot) { "$classFqName is root when processing $change" }

                val scope = classFqName.parent().asString()
                val name = classFqName.shortName().identifier
                dirtyLookupSymbols.add(LookupSymbol(name, scope))
            }
        }
        else if (change is ChangeInfo.MembersChanged) {
            val fqNames = withSubtypes(change.fqName, caches)
            // need to recompile subtypes because changed member might break override
            dirtyClassesFqNames.addAll(fqNames)

            for (name in change.names) {
                fqNames.mapTo(dirtyLookupSymbols) { LookupSymbol(name, it.asString()) }
            }

            fqNames.mapTo(dirtyLookupSymbols) { LookupSymbol(SAM_LOOKUP_NAME.asString(), it.asString()) }
        }
    }

    return DirtyData(dirtyLookupSymbols, dirtyClassesFqNames)
}

fun mapLookupSymbolsToFiles(
        lookupStorage: LookupStorage,
        lookupSymbols: Iterable<LookupSymbol>,
        reporter: ICReporter,
        excludes: Set<File> = emptySet()
): Set<File> {
    val dirtyFiles = HashSet<File>()

    for (lookup in lookupSymbols) {
        val affectedFiles = lookupStorage.get(lookup).map(::File).filter { it !in excludes }
        reporter.report { "${lookup.scope}#${lookup.name} caused recompilation of: ${reporter.pathsAsString(affectedFiles)}" }
        dirtyFiles.addAll(affectedFiles)
    }

    return dirtyFiles
}

fun mapClassesFqNamesToFiles(
    caches: Iterable<IncrementalCacheCommon>,
    classesFqNames: Iterable<FqName>,
    reporter: ICReporter,
    excludes: Set<File> = emptySet()
): Set<File> {
    val dirtyFiles = HashSet<File>()

    for (cache in caches) {
        for (dirtyClassFqName in classesFqNames) {
            val srcFile = cache.getSourceFileIfClass(dirtyClassFqName)
            if (srcFile == null || srcFile in excludes || srcFile.isJavaFile()) continue

            reporter.report { ("Class $dirtyClassFqName caused recompilation of: ${reporter.pathsAsString(srcFile)}") }
            dirtyFiles.add(srcFile)
        }
    }

    return dirtyFiles
}

fun withSubtypes(
        typeFqName: FqName,
        caches: Iterable<IncrementalCacheCommon>
): Set<FqName> {
    val types = LinkedList(listOf(typeFqName))
    val subtypes = hashSetOf<FqName>()

    while (types.isNotEmpty()) {
        val unprocessedType = types.pollFirst()

        caches.asSequence()
              .flatMap { it.getSubtypesOf(unprocessedType) }
              .filter { it !in subtypes }
              .forEach { types.addLast(it) }

        subtypes.add(unprocessedType)
    }

    return subtypes
}

