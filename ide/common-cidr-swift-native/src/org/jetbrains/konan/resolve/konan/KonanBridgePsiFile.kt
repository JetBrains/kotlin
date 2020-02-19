/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.resolve.konan

import com.intellij.extapi.psi.LightPsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.source.LightPsiFileImpl
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCFileType
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cidr.lang.OCLanguageKind
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContextUtil
import com.jetbrains.cidr.lang.psi.OCConfigurationOwner
import com.jetbrains.cidr.lang.psi.OCParsedLanguageAndConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration

class KonanBridgePsiFile(val target: KonanTarget, provider: SingleRootFileViewProvider) :
    LightPsiFileBase(provider, OCLanguage.getInstance()), PsiFile, OCConfigurationOwner {

    override fun clearCaches() {}

    override fun getChildren(): Array<PsiElement> = PsiElement.EMPTY_ARRAY

    override fun copyLight(viewProvider: FileViewProvider): LightPsiFileImpl? = null

    override fun getFileType(): FileType = OCFileType.INSTANCE

    override fun getRootKind(config: OCResolveConfiguration?): OCLanguageKind = CLanguageKind.OBJ_C

    override fun getParsedLanguageAndConfiguration(): OCParsedLanguageAndConfiguration? {
        val configuration = OCInclusionContextUtil.getResolveRootAndActiveConfiguration(virtualFile, project).configuration
        return OCParsedLanguageAndConfiguration(configuration, getRootKind(configuration))
    }
}