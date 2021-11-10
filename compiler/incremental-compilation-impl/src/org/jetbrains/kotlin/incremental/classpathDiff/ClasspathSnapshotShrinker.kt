/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.BuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.incremental.LookupStorage
import org.jetbrains.kotlin.incremental.storage.LookupSymbolKey
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

object ClasspathSnapshotShrinker {

    /**
     * Shrinks the given [ClasspathSnapshot] by retaining only classes that are referenced. Referencing info is stored in [LookupStorage].
     *
     * This method also removes duplicate classes and [EmptyJavaClassSnapshot]s first.
     */
    fun shrink(
        classpathSnapshot: ClasspathSnapshot,
        lookupStorage: LookupStorage,
        metrics: BuildMetricsReporter
    ): List<ClassSnapshotWithHash> {
        val allClasses = metrics.measure(BuildTime.GET_NON_DUPLICATE_CLASSES) {
            // It's important to remove duplicate classes first before removing `EmptyJavaClassSnapshot`s.
            // For example, if jar1!/com/example/A.class is empty and jar2!/com/example/A.class is non-empty, incorrect order of the actions
            // will lead to incorrect results.
            classpathSnapshot.getNonDuplicateClassSnapshots().filter { it.classSnapshot !is EmptyJavaClassSnapshot }
        }
        val lookupSymbols = metrics.measure(BuildTime.GET_LOOKUP_SYMBOLS) {
            lookupStorage.lookupMap.keys
        }
        val referencedClasses = metrics.measure(BuildTime.FIND_REFERENCED_CLASSES) {
            findReferencedClasses(allClasses, lookupSymbols)
        }
        return metrics.measure(BuildTime.FIND_TRANSITIVELY_REFERENCED_CLASSES) {
            findTransitivelyReferencedClasses(allClasses, referencedClasses)
        }
    }

    /**
     * Finds classes that are referenced. Referencing info is stored in [LookupStorage].
     *
     * Note: It's okay to over-approximate referenced classes.
     */
    private fun findReferencedClasses(
        allClasses: List<ClassSnapshotWithHash>,
        lookupSymbols: Collection<LookupSymbolKey>
    ): List<ClassSnapshotWithHash> {
        val potentialClassNamesOfReferencedClasses =
            lookupSymbols.flatMap {
                val lookupSymbolFqName = if (it.scope.isEmpty()) FqName(it.name) else FqName("${it.scope}.${it.name}")
                listOf(
                    lookupSymbolFqName, // If LookupSymbol refers to a class, the class's FqName will be captured here.
                    FqName(it.scope) // If LookupSymbol refers to a class member, the class's FqName will be captured here.
                )
            }.toSet() // Use Set for presence check
        val potentialPackageNamesOfReferencedPackageLevelMembers =
            lookupSymbols.map {
                FqName(it.scope) // If LookupSymbol refers to a package-level member, the package's FqName will be captured here.
            }.toSet() // Use Set for presence check

        return allClasses.filter {
            val classId = it.classSnapshot.getClassId()

            (classId.asSingleFqName() in potentialClassNamesOfReferencedClasses) ||
                    (it.classSnapshot is KotlinClassSnapshot
                            && it.classSnapshot.classInfo.classKind != KotlinClassHeader.Kind.CLASS
                            && classId.packageFqName in potentialPackageNamesOfReferencedPackageLevelMembers)
        }
    }

    /**
     * Finds classes that are transitively referenced. For example, if a subclass is referenced, its supertypes will potentially be
     * referenced.
     *
     * The returned list includes the given referenced classes plus the transitively referenced ones.
     */
    private fun findTransitivelyReferencedClasses(
        allClasses: List<ClassSnapshotWithHash>,
        referencedClasses: List<ClassSnapshotWithHash>
    ): List<ClassSnapshotWithHash> {
        val classIdToClassSnapshot = allClasses.associateBy { it.classSnapshot.getClassId() }
        val classIds: Set<ClassId> = classIdToClassSnapshot.keys // Use Set for presence check
        val classNameToClassId = classIds.associateBy { JvmClassName.byClassId(it) }
        val classNameToClassIdResolver = { className: JvmClassName -> classNameToClassId[className] }

        val supertypesResolver = { classId: ClassId ->
            // No need to collect supertypes outside the given set of classes (e.g., "java/lang/Object")
            @Suppress("SimpleRedundantLet")
            classIdToClassSnapshot[classId]?.let {
                it.classSnapshot.getSupertypes(classNameToClassIdResolver).filter { supertype -> supertype in classIds }.toSet()
            } ?: emptySet()
        }

        val referencedClassIds = referencedClasses.map { it.classSnapshot.getClassId() }.toSet()
        val transitivelyReferencedClassIds: Set<ClassId> =
            ImpactAnalysis.findImpactedClassesInclusive(referencedClassIds, supertypesResolver) // Use Set for presence check

        return allClasses.filter { it.classSnapshot.getClassId() in transitivelyReferencedClassIds }
    }
}

/**
 * Returns all [ClassSnapshot]s in this [ClasspathSnapshot].
 *
 * If there are duplicate classes on the classpath, retain only the first one to match the compiler's behavior.
 */
internal fun ClasspathSnapshot.getNonDuplicateClassSnapshots(): List<ClassSnapshotWithHash> {
    val classSnapshots = LinkedHashMap<String, ClassSnapshotWithHash>(classpathEntrySnapshots.sumOf { it.classSnapshots.size })
    for (classpathEntrySnapshot in classpathEntrySnapshots) {
        for ((unixStyleRelativePath, classSnapshot) in classpathEntrySnapshot.classSnapshots) {
            classSnapshots.putIfAbsent(unixStyleRelativePath, classSnapshot)
        }
    }
    return classSnapshots.values.toList()
}
