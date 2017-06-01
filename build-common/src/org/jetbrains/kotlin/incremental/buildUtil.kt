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

// these functions are used in the kotlin gradle plugin
@file:Suppress("unused")

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.build.*
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollectorImpl
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File
import java.util.*


fun Iterable<File>.javaSourceRoots(roots: Iterable<File>): Iterable<File> =
        filter(File::isJavaFile)
                .map { findSrcDirRoot(it, roots) }
                .filterNotNull()

fun makeModuleFile(name: String, isTest: Boolean, outputDir: File, sourcesToCompile: Iterable<File>, javaSourceRoots: Iterable<File>, classpath: Iterable<File>, friendDirs: Iterable<File>): File {
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

fun makeCompileServices(
        incrementalCaches: Map<TargetId, IncrementalCache>,
        lookupTracker: LookupTracker,
        compilationCanceledStatus: CompilationCanceledStatus?
): Services =
    with(Services.Builder()) {
        register(IncrementalCompilationComponents::class.java, 
                 IncrementalCompilationComponentsImpl(incrementalCaches, lookupTracker))
        compilationCanceledStatus?.let {
            register(CompilationCanceledStatus::class.java, it)
        }
        build()
    }

fun makeLookupTracker(parentLookupTracker: LookupTracker = LookupTracker.DO_NOTHING): LookupTracker =
        if (IncrementalCompilation.isExperimental()) LookupTrackerImpl(parentLookupTracker)
        else parentLookupTracker

fun<Target> makeIncrementalCachesMap(
        targets: Iterable<Target>,
        getDependencies: (Target) -> Iterable<Target>,
        getCache: (Target) -> IncrementalCacheImpl<Target>,
        getTargetId: Target.() -> TargetId
): Map<TargetId, IncrementalCacheImpl<Target>>
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

fun<Target> updateIncrementalCaches(
        targets: Iterable<Target>,
        generatedFiles: List<GeneratedFile<Target>>,
        compiledWithErrors: Boolean,
        getIncrementalCache: (Target) -> IncrementalCacheImpl<Target>
): CompilationResult {

    var changesInfo = CompilationResult.NO_CHANGES
    for (generatedFile in generatedFiles) {
        val ic = getIncrementalCache(generatedFile.target)
        when {
            generatedFile is GeneratedJvmClass<Target> -> changesInfo += ic.saveFileToCache(generatedFile)
            generatedFile.outputFile.isModuleMappingFile() -> changesInfo += ic.saveModuleMappingToCache(generatedFile.sourceFiles, generatedFile.outputFile)
        }
    }

    if (!compiledWithErrors) {
        targets.forEach {
            val newChangesInfo = getIncrementalCache(it).clearCacheForRemovedClasses()
            changesInfo += newChangesInfo
        }
    }

    return changesInfo
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

fun<Target> OutputItemsCollectorImpl.generatedFiles(
        targets: Collection<Target>,
        representativeTarget: Target,
        getSources: (Target) -> Iterable<File>,
        getOutputDir: (Target) -> File?
): List<GeneratedFile<Target>> {
    // If there's only one target, this map is empty: get() always returns null, and the representativeTarget will be used below
    val sourceToTarget =
            if (targets.size >1) targets.flatMap { target -> getSources(target).map { Pair(it, target) } }.toMap()
            else mapOf<File, Target>()

    return outputs.map { outputItem ->
        val target =
                outputItem.sourceFiles.firstOrNull()?.let { sourceToTarget[it] } ?:
                targets.filter { getOutputDir(it)?.let { outputItem.outputFile.startsWith(it) } ?: false }.singleOrNull() ?:
                representativeTarget

        when (outputItem.outputFile.extension) {
            "class" -> GeneratedJvmClass(target, outputItem.sourceFiles, outputItem.outputFile)
            else -> GeneratedFile(target, outputItem.sourceFiles, outputItem.outputFile)
        }
    }
}

data class DirtyData(
        val dirtyLookupSymbols: Collection<LookupSymbol> = emptyList(),
        val dirtyClassesFqNames: Collection<FqName> = emptyList()
)

fun <Target> CompilationResult.getDirtyData(
        caches: Iterable<IncrementalCacheImpl<Target>>,
        reporter: ICReporter
): DirtyData {
    val dirtyLookupSymbols = HashSet<LookupSymbol>()
    val dirtyClassesFqNames = HashSet<FqName>()

    for (change in changes) {
        reporter.report { "Process $change" }

        if (change is ChangeInfo.SignatureChanged) {
            val fqNames = if (!change.areSubclassesAffected) listOf(change.fqName) else withSubtypes(change.fqName, caches)

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
                for (fqName in fqNames) {
                    dirtyLookupSymbols.add(LookupSymbol(name, fqName.asString()))
                }
            }
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

fun <Target> mapClassesFqNamesToFiles(
        caches: Iterable<IncrementalCacheImpl<Target>>,
        classesFqNames: Iterable<FqName>,
        reporter: ICReporter,
        excludes: Set<File> = emptySet()
): Set<File> {
    val dirtyFiles = HashSet<File>()

    for (cache in caches) {
        for (dirtyClassFqName in classesFqNames) {
            val srcFile = cache.getSourceFileIfClass(dirtyClassFqName)
            if (srcFile == null || srcFile in excludes) continue

            reporter.report { ("Class $dirtyClassFqName caused recompilation of: ${reporter.pathsAsString(srcFile)}") }
            dirtyFiles.add(srcFile)
        }
    }

    return dirtyFiles
}

private fun findSrcDirRoot(file: File, roots: Iterable<File>): File? =
        roots.firstOrNull { FileUtil.isAncestor(it, file, false) }

fun <Target> withSubtypes(
        typeFqName: FqName,
        caches: Iterable<IncrementalCacheImpl<Target>>
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

