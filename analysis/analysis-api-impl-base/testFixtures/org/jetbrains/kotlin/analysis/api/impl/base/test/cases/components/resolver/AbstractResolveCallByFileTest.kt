/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolver

import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResolveCallByFileTest : AbstractResolveCallTest() {
    override fun collectElementsToResolve(
        file: KtFile,
        module: KtTestModule,
        testServices: TestServices,
    ): Collection<ResolveTestCaseContext<KtElement>> = collectAllKtElements(file)
}
