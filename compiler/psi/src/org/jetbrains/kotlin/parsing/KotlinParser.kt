/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParsing.Companion.createForTopLevel

class KotlinParser(project: Project?) : PsiParser {
    override fun parse(iElementType: IElementType, psiBuilder: PsiBuilder): ASTNode {
        throw IllegalStateException("use another parse")
    }

    companion object {
        // we need this method because we need psiFile
        @JvmStatic
        fun parse(psiBuilder: PsiBuilder, psiFile: PsiFile): ASTNode {
            val ktParsing = createForTopLevel(SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder))
            val extension = FileUtilRt.getExtension(psiFile.getName())
            if (extension.isEmpty() || extension == KotlinFileType.EXTENSION || isCompiledFile(psiFile)) {
                ktParsing.parseFile()
            } else {
                ktParsing.parseScript()
            }
            return psiBuilder.getTreeBuilt()
        }

        @Suppress("deprecation")
        private fun isCompiledFile(psiFile: PsiFile?): Boolean {
            //avoid loading KtFile which depends on java psi, which is not available in some setup
            //e.g. remote dev https://youtrack.jetbrains.com/issue/GTW-7554
            return psiFile is org.jetbrains.kotlin.psi.KtCommonFile && psiFile.isCompiled
        }

        @JvmStatic
        fun parseTypeCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val ktParsing = createForTopLevel(SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder))
            ktParsing.parseTypeCodeFragment()
            return psiBuilder.getTreeBuilt()
        }

        @JvmStatic
        fun parseExpressionCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val ktParsing = createForTopLevel(SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder))
            ktParsing.parseExpressionCodeFragment()
            return psiBuilder.getTreeBuilt()
        }

        @JvmStatic
        fun parseBlockCodeFragment(psiBuilder: PsiBuilder): ASTNode {
            val ktParsing = createForTopLevel(SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder))
            ktParsing.parseBlockCodeFragment()
            return psiBuilder.getTreeBuilt()
        }

        @JvmStatic
        fun parseLambdaExpression(psiBuilder: PsiBuilder): ASTNode {
            val ktParsing = createForTopLevel(SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder))
            ktParsing.parseLambdaExpression()
            return psiBuilder.getTreeBuilt()
        }

        fun parseBlockExpression(psiBuilder: PsiBuilder): ASTNode {
            val ktParsing = createForTopLevel(SemanticWhitespaceAwarePsiBuilderImpl(psiBuilder))
            ktParsing.parseBlockExpression()
            return psiBuilder.getTreeBuilt()
        }
    }
}
