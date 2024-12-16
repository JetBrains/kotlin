/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path

/**
 * Creates a [TestSymbolTarget] from the test data file at [testDataPath] and resolves all [KtElement]s from it using the
 * [KtElementTestSymbolTargetResolver].
 *
 * @param contextFile The [KtFile] from which the [TestSymbolTarget] should be resolved. See [TestSymbolTarget.create].
 */
fun getTestTargetKtElements(testDataPath: Path, contextFile: KtFile): List<KtElement> {
    val target = TestSymbolTarget.parse(testDataPath, contextFile)
    return KtElementTestSymbolTargetResolver(contextFile.project).resolveTarget(target)
}
