/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNonPublicApi
import org.jetbrains.kotlin.psi.markAsReplSnippet

abstract class AbstractRawFirBuilderLazyBodiesByAstTest : AbstractRawFirBuilderLazyBodiesTestCase() {
    @OptIn(KtNonPublicApi::class)
    override fun createKtFile(filePath: String): KtFile {
        val file = super.createKtFile(filePath)
        if (filePath.endsWith(".repl.kts")) file.script?.markAsReplSnippet()
        file.calcTreeElement()
        assertNotNull("Ast tree for the file must be not null", file.treeElement)
        return file
    }
}
