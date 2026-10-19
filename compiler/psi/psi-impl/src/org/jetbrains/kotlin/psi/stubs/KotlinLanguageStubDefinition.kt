/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.LightLanguageStubDefinition
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtFileLightStubBuilder

/**
 * Kotlin builds stubs from a light tree (see [KtFileLightStubBuilder]): during indexing the file is parsed with
 * [parseContentsLight] without materializing the PSI tree, and for files with a loaded AST the same builder works
 * over a tree-backed light view.
 */
internal class KotlinLanguageStubDefinition : LightLanguageStubDefinition {
    override val stubVersion: Int
        get() = KotlinStubVersions.SOURCE_STUB_VERSION

    override val builder: StubBuilder
        get() = KtFileLightStubBuilder()

    override fun parseContentsLight(chameleon: ASTNode): FlyweightCapableTreeStructure<LighterASTNode> {
        val psi = chameleon.psi
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            psi.project,
            chameleon,
            null,
            KotlinLanguage.INSTANCE,
            chameleon.chars,
        )

        // Mirrors the file-vs-script decision of `KotlinParser.parse`
        val file = psi.containingFile
        val extension = FileUtilRt.getExtension(file.name)
        val isScript = !extension.isEmpty() &&
                extension != KotlinFileType.EXTENSION &&
                (file as? KtFile)?.isCompiled != true

        return KotlinLightParser.parse(builder, isScript)
    }
}
