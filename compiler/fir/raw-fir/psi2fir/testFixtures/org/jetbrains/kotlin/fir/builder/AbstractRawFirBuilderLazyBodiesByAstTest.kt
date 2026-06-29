/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.assertNotNull

abstract class AbstractRawFirBuilderLazyBodiesByAstTest : AbstractRawFirBuilderLazyBodiesTestCase() {
    override fun createKtFile(filePath: String): KtFile {
        val file = super.createKtFile(filePath)
        file.calcTreeElement()
        assertNotNull(file.treeElement) { "Ast tree for the file must be not null" }
        return file
    }
}
