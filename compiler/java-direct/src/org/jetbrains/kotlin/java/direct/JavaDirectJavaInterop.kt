/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForModule
import org.jetbrains.kotlin.fir.session.FirJavaInterop
import org.jetbrains.kotlin.java.direct.resolution.JavaModuleImportedPackagesOverModuleGraph
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileScope
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import java.util.IdentityHashMap
import org.jetbrains.kotlin.search.AbstractProjectFileSearchScope

/**
 * The java-direct Java view of a compilation, to be put into `FirJvmSessionFactory.Context`.
 *
 * [FirJavaInterop.registerKotlinDeclarationsForJava] stays a no-op: Java is resolved here without PSI, so
 * there is no PSI Java resolution which would need the Kotlin declarations as stubs.
 */
fun createJavaDirectJavaInterop(
    javaSourceRoots: List<JavaSourceRootEntry>,
    binaryClasses: BinaryJavaClassCache,
    binaryClassFileScopeFor: (AbstractProjectFileSearchScope) -> BinaryClassFileScope,
    javaModuleFinder: JavaModuleFinder,
    javaSourcesScope: AbstractProjectFileSearchScope,
): FirJavaInterop {
    val moduleImportedPackages = JavaModuleImportedPackagesOverModuleGraph(javaModuleFinder)

    // Indexed by search scope identity.
    val binaryFinders: MutableMap<AbstractProjectFileSearchScope, JavaClassFinder> = IdentityHashMap()

    return object : FirJavaInterop {
        override fun createJavaFacade(
            session: FirSession,
            moduleData: FirModuleData,
            fileSearchScope: AbstractProjectFileSearchScope,
        ): FirJavaFacade {
            val finder: JavaClassFinder = when {
                fileSearchScope === javaSourcesScope ->
                    JavaClassFinderOverAstImpl(session, javaSourceRoots, moduleImportedPackages)
                else -> binaryFinders.getOrPut(fileSearchScope) {
                    JavaClassFinderOverBinaryIndex(binaryClasses, binaryClassFileScopeFor(fileSearchScope))
                }
            }
            return FirJavaFacadeForModule(session, moduleData, finder)
        }
    }
}
