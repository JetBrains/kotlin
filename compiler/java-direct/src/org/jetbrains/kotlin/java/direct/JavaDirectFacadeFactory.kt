/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.fir.java.FirJavaFacadeForModule
import org.jetbrains.kotlin.fir.session.FirJavaFacadeFactory
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.java.direct.resolution.JavaModuleImportedPackagesOverModuleGraph
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileScope
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClassCache
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import java.util.IdentityHashMap

/**
 * The java-direct Java view of a compilation, to be put into `FirJvmSessionFactory.Context`.
 */
fun createJavaDirectJavaFacadeFactory(
    javaSourceRoots: List<JavaSourceRootEntry>,
    binaryClasses: BinaryJavaClassCache,
    binaryClassFileScopeFor: (AbstractProjectFileSearchScope) -> BinaryClassFileScope,
    javaModuleFinder: JavaModuleFinder,
    javaSourcesScope: AbstractProjectFileSearchScope,
): FirJavaFacadeFactory {
    val moduleImportedPackages = JavaModuleImportedPackagesOverModuleGraph(javaModuleFinder)

    // Indexed by search scope identity.
    val binaryFinders: MutableMap<AbstractProjectFileSearchScope, JavaClassFinder> = IdentityHashMap()

    return FirJavaFacadeFactory { session, moduleData, scope ->
        val finder: JavaClassFinder = when {
            scope === javaSourcesScope -> JavaClassFinderOverAstImpl(session, javaSourceRoots, moduleImportedPackages)
            else -> binaryFinders.getOrPut(scope) {
                JavaClassFinderOverBinaryIndex(binaryClasses, binaryClassFileScopeFor(scope))
            }
        }
        FirJavaFacadeForModule(session, moduleData, finder)
    }
}
