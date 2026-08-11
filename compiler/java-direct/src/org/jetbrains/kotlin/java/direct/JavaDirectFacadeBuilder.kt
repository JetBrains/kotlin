/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForModule
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.java.direct.resolution.JavaModuleImportedPackagesOverModuleGraph
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileScope
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import java.util.IdentityHashMap

/**
 * Injects java-direct into FIR JVM sessions via `createJavaFacade`.
 */
fun createJavaDirectJavaFacadeBuilder(
    javaSourceRoots: List<JavaSourceRootEntry>,
    binaryClasses: BinaryJavaClassCache,
    binaryClassFileScopeFor: (AbstractProjectFileSearchScope) -> BinaryClassFileScope,
    javaModuleFinder: JavaModuleFinder,
    javaSourcesScope: AbstractProjectFileSearchScope,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val moduleImportedPackages = JavaModuleImportedPackagesOverModuleGraph(javaModuleFinder)

    // Indexed by search scope identity.
    val binaryFinders: MutableMap<AbstractProjectFileSearchScope, JavaClassFinder> = IdentityHashMap()

    return { _, session, moduleData, scope ->
        val finder: JavaClassFinder = when {
            scope === javaSourcesScope -> JavaClassFinderOverAstImpl(session, javaSourceRoots, moduleImportedPackages)
            else -> binaryFinders.getOrPut(scope) {
                JavaClassFinderOverBinaryIndex(binaryClasses, binaryClassFileScopeFor(scope))
            }
        }
        FirJavaFacadeForModule(session, moduleData, finder)
    }
}
