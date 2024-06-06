/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResolveReferenceByFileTest : AbstractResolveReferenceTest() {
    override fun collectElementsToResolve(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtReference?>> = collectAllReferences(mainFile)
}
