/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.lang.Language
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettingsFacade
import com.intellij.psi.codeStyle.JavaFileCodeStyleFacade
import com.intellij.psi.codeStyle.JavaFileCodeStyleFacadeFactory

internal class DummyJavaFileCodeStyleFacadeFactory : JavaFileCodeStyleFacadeFactory {
    private class DummyJavaFileCodeStyleFacade : JavaFileCodeStyleFacade {
        override fun getNamesCountToUseImportOnDemand(): Int = 0
        override fun isToImportOnDemand(qualifiedName: String): Boolean = false
        override fun useFQClassNames(): Boolean = false
        override fun isJavaDocLeadingAsterisksEnabled(): Boolean = false
        override fun isGenerateFinalParameters(): Boolean = false
        override fun isGenerateFinalLocals(): Boolean = false
        override fun withLanguage(language: Language): CodeStyleSettingsFacade = DummyJavaFileCodeStyleFacade()
        override fun getTabSize(): Int = 4
        override fun getIndentSize(): Int = 4
        override fun isSpaceBeforeComma(): Boolean = false
        override fun isSpaceAfterComma(): Boolean = false
        override fun isSpaceAroundAssignmentOperators(): Boolean = false
    }

    override fun createFacade(psiFile: PsiFile): JavaFileCodeStyleFacade = DummyJavaFileCodeStyleFacade()
}