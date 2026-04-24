/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.FqName

/**
 * A JavaClassFinder that combines source-based lookup (java-direct) with binary-based lookup (platform).
 * 
 * Resolution order:
 * 1. Try source finder (JavaClassFinderOverAstImpl) - for Java sources in the current module
 * 2. Fall back to binary finder (platform-based) - for JDK, libraries, binary dependencies
 * 
 * This hybrid approach allows java-direct to handle source parsing while still accessing
 * binary dependencies through the existing platform infrastructure.
 */
class CombinedJavaClassFinder(
    private val sourceFinder: JavaClassFinderOverAstImpl,
    private val binaryFinder: JavaClassFinder,
) : JavaClassFinder {

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        if (sourceFinder.isClassInIndex(request.classId)) {
            val fromSource = sourceFinder.findClass(request)
            if (fromSource != null) return fromSource
        }
        val fromBinary = binaryFinder.findClass(request)

        // Verify the FQN: some underlying finders (notably PSI) may return a class whose FQN
        // does not match the requested ClassId — e.g. an inner class of an unrelated outer.
        // ClassId.asSingleFqName() flattens nested-class separators, so this is a flat-FQName
        // equality, not a structural ClassId comparison.
        if (fromBinary != null) {
            val expectedFqName = request.classId.asSingleFqName()
            val actualFqName = fromBinary.fqName
            if (actualFqName != expectedFqName) {
                return null
            }
        }

        return fromBinary
    }

    override fun findClasses(request: JavaClassFinder.Request): List<JavaClass> {
        if (sourceFinder.isClassInIndex(request.classId)) {
            val fromSources = sourceFinder.findClasses(request)
            if (fromSources.isNotEmpty()) return fromSources
        }

        return binaryFinder.findClasses(request)
    }

    override fun findPackage(fqName: FqName, mayHaveAnnotations: Boolean): JavaPackage? {
        return sourceFinder.findPackage(fqName, mayHaveAnnotations)
            ?: binaryFinder.findPackage(fqName, mayHaveAnnotations)
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String> {
        val fromSources = sourceFinder.knownClassNamesInPackage(packageFqName)
        val fromBinaries = binaryFinder.knownClassNamesInPackage(packageFqName)

        return when {
            fromBinaries == null -> fromSources
            else -> fromSources + fromBinaries
        }
    }

    override fun canComputeKnownClassNamesInPackage(): Boolean {
        return sourceFinder.canComputeKnownClassNamesInPackage() || binaryFinder.canComputeKnownClassNamesInPackage()
    }
}
