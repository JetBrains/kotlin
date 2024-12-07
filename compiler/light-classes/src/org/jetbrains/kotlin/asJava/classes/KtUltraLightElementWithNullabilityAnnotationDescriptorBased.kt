/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.nullability

interface KtUltraLightElementWithNullabilityAnnotationDescriptorBased<T : KtDeclaration, D : PsiModifierListOwner> :
    KtUltraLightElementWithNullabilityAnnotation<T, D> {

    fun computeQualifiedNameForNullabilityAnnotation(kotlinType: KotlinType?): String? {
        return computeNullabilityQualifier(kotlinType, psiTypeForNullabilityAnnotation)
    }
}

fun computeNullabilityQualifier(kotlinType: KotlinType?, psiType: PsiType?): String? {
    if (psiType == null || psiType is PsiPrimitiveType) return null

    val notErrorKotlinType = kotlinType?.takeUnless(KotlinType::isError) ?: return null
    if (notErrorKotlinType.isTypeParameter()) {
        if (!TypeUtils.hasNullableSuperType(notErrorKotlinType)) return JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION.asString()
        if (!notErrorKotlinType.isMarkedNullable) return null
    }

    return when (notErrorKotlinType.nullability()) {
        TypeNullability.NOT_NULL -> JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION.asString()
        TypeNullability.NULLABLE -> JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION.asString()
        TypeNullability.FLEXIBLE -> null
    }
}