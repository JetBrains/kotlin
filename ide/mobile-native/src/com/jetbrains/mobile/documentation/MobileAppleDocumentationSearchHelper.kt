package com.jetbrains.mobile.documentation

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.documentation.search.candidates.XcodeDocumentationCandidateBasedSearchHelper
import com.jetbrains.cidr.documentation.search.candidates.XcodeDocumentationTypesComparator

class MobileAppleDocumentationSearchHelper : XcodeDocumentationCandidateBasedSearchHelper {
    override fun getPlatformName(psiElement: PsiElement): String? = "ios"

    override val typesComparator: XcodeDocumentationTypesComparator? = null
}
