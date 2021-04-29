/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.types.typeUtil.closure
import java.io.File

/**
 * The idea behind this test is to check that [FirIdeSealedHierarchyProcessor] finds all direct inheritors of sealed classes and interfaces.
 * We use the fact that [SealedClassInheritorsKt#getSealedInheritors] property gets its value thanks to the class activity.
 *
 * Inheritors are collected for every sealed declaration of the 'fileToResolve' (see test data 'structure.json'). Resulting collection is
 * compared with 'expected.txt'.
 */
abstract class AbstractFirSealedInheritorsTest : AbstractFirMultiModuleLazyResolveTest() {
    override fun getTestDataPath(): String =
        "${KtTestUtil.getHomeDirectory()}/idea/idea-frontend-fir/idea-fir-low-level-api/testdata/resolveSealed/"

    override fun checkFirFile(firFile: FirFile, path: String) {
        val allClasses = firFile.listNestedClasses().closure { it.listNestedClasses() }
        val inheritorNames = allClasses.flatMap { firClass ->
            if (firClass.isSealed) {
                firClass.getSealedClassInheritors(firFile.moduleData.session)
            } else {
                emptyList()
            }
        }.map { it.asString() }.sorted()
        KotlinTestUtils.assertEqualsToFile(File("$path/expected.txt"), inheritorNames.joinToString("\n"))
    }
}

private fun FirDeclaration.listNestedClasses(): List<FirRegularClass> {
    return when (this) {
        is FirFile -> declarations.filterIsInstance<FirRegularClass>()
        is FirRegularClass -> declarations.filterIsInstance<FirRegularClass>()
        else -> emptyList()
    }
}
