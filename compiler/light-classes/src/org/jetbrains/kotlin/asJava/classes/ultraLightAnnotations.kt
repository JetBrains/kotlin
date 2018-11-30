package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.asJava.elements.KtLightNullabilityAnnotation
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.nullability

class KtUltraLightNullabilityAnnotation(
    member: KtUltraLightElementWithNullabilityAnnotation<*, *>,
    parent: PsiElement
) : KtLightNullabilityAnnotation<KtUltraLightElementWithNullabilityAnnotation<*, *>>(member, parent) {
    override fun getQualifiedName(): String? {
        val kotlinType = member.kotlinTypeForNullabilityAnnotation?.takeUnless(KotlinType::isError) ?: return null
        val psiType = member.psiTypeForNullabilityAnnotation ?: return null
        if (member.isPrivateOrParameterInPrivateMethod() || psiType is PsiPrimitiveType) return null

        if (kotlinType.isTypeParameter()) {
            if (!TypeUtils.hasNullableSuperType(kotlinType)) return NotNull::class.java.name
            if (!kotlinType.isMarkedNullable) return null
        }

        val nullability = kotlinType.nullability()
        return when (nullability) {
            TypeNullability.NOT_NULL -> NotNull::class.java.name
            TypeNullability.NULLABLE -> Nullable::class.java.name
            TypeNullability.FLEXIBLE -> null
        }
    }
}

fun PsiModifierListOwner.isPrivateOrParameterInPrivateMethod(): Boolean {
    if (hasModifier(JvmModifier.PRIVATE)) return true
    if (this !is PsiParameter) return false
    val parentMethod = declarationScope as? PsiMethod ?: return false
    return parentMethod.hasModifier(JvmModifier.PRIVATE)
}
