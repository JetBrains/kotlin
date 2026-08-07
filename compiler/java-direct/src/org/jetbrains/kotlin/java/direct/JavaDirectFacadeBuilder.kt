/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.java.FirJavaFacadeWithFixedModuleData
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.java.direct.resolution.JavaModuleImportedPackagesOverModuleGraph
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassRoots
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import java.util.IdentityHashMap

/**
 * Injects java-direct into FIR JVM sessions via `createJavaFacade` when `useJavaDirect` is set.
 *
 * Every session gets a single-sided [JavaClassFinder]. The source scope is the identified case
 * (`scope === javaSourcesScope` reads `.java` files through [JavaClassFinderOverAstImpl]); every
 * other scope is binary and is served by a [JavaClassFinderOverBinaryIndex] over the binary roots
 * as seen through that very scope.
 */
fun createJavaDirectJavaFacadeBuilder(
    javaSourceRoots: List<JavaSourceRootEntry>,
    binaryClassRootsForScope: (AbstractProjectFileSearchScope) -> BinaryClassRoots,
    javaModuleFinder: JavaModuleFinder,
    javaSourcesScope: AbstractProjectFileSearchScope,
): (AbstractProjectEnvironment, FirSession, FirModuleData, AbstractProjectFileSearchScope) -> FirJavaFacade {
    val moduleImportedPackages = JavaModuleImportedPackagesOverModuleGraph(javaModuleFinder)

    // Keyed by scope identity: distinct binary scopes must get distinct finders, and the same scope
    // object must reuse its finder (and hence its caches).
    val binaryFinders: MutableMap<AbstractProjectFileSearchScope, JavaClassFinder> = IdentityHashMap()

    return { _, session, moduleData, scope ->
        val finder: JavaClassFinder = when {
            scope === javaSourcesScope -> JavaClassFinderOverAstImpl(session, javaSourceRoots, moduleImportedPackages)
            else -> binaryFinders.getOrPut(scope) { JavaClassFinderOverBinaryIndex(binaryClassRootsForScope(scope)) }
        }
        FirJavaFacadeWithFixedModuleData(session, moduleData, finder)
    }
}
