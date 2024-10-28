/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * A code fragment which may have import and package statements but at the same time may have top-level expressions
 *
 * It has no context `PsiElement`, so it's resolved as a file.
 */
class KtFileLikeCodeFragment(
    viewProvider: FileViewProvider,
    imports: String?, // Should be separated by KtCodeFragment.IMPORT_SEPARATOR
) : KtCodeFragment(viewProvider, imports, KtNodeTypes.FILE_LIKE_CODE_FRAGMENT, context = null) {

    constructor(
        project: Project,
        name: String,
        text: CharSequence,
        imports: String?,
    ) : this(
        createFileViewProviderForLightFile(project, name, text),
        imports,
    )

    override val importLists: List<KtImportList>
        get() = buildList {
            addAll(findChildrenByTypeOrClass(KtStubElementTypes.IMPORT_LIST, KtImportList::class.java))
            addIfNotNull(importsAsImportList())
        }

    override fun getContentElement() = findChildByClass(KtBlockExpression::class.java)
        ?: throw IllegalStateException("Block expression should be parsed for BlockCodeFragment")
}
