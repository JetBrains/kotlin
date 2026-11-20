/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.java

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation

/**
 * Carries a [JavaAnnotation] through platform components which provide [PsiAnnotation]s.
 */
@KaImplementationDetail
class KaFirJavaAnnotationPsi(
    val javaAnnotation: JavaAnnotation,
    manager: PsiManager,
) : LightElement(manager, JavaLanguage.INSTANCE), PsiAnnotation {
    override fun getQualifiedName(): String? = javaAnnotation.classId?.asFqNameString()

    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement? = null

    override fun getParameterList(): PsiAnnotationParameterList = throw UnsupportedOperationException()

    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? = null

    override fun findDeclaredAttributeValue(attributeName: String?): PsiAnnotationMemberValue? = null

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?): T =
        throw UnsupportedOperationException()

    override fun getOwner(): PsiAnnotationOwner? = null

    override fun toString(): String = "KaFirJavaAnnotationPsi($qualifiedName)"
}
