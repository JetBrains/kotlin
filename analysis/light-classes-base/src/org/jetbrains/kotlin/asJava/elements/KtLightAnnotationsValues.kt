/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtElement

class KtLightPsiArrayInitializerMemberValue(
    override val kotlinOrigin: KtElement,
    private val lightParent: PsiElement,
    private val arguments: (KtLightPsiArrayInitializerMemberValue) -> List<PsiAnnotationMemberValue>
) : KtLightElementBase(lightParent), PsiArrayInitializerMemberValue {
    override fun getInitializers(): Array<PsiAnnotationMemberValue> = arguments(this).toTypedArray()

    override fun getParent(): PsiElement = lightParent

    override fun isPhysical(): Boolean = false
}

fun psiType(kotlinFqName: String, context: PsiElement, boxPrimitiveType: Boolean = false): PsiType {
    if (!boxPrimitiveType) {
        when (kotlinFqName) {
            "kotlin.Int" -> return PsiType.INT
            "kotlin.Long" -> return PsiType.LONG
            "kotlin.Short" -> return PsiType.SHORT
            "kotlin.Boolean" -> return PsiType.BOOLEAN
            "kotlin.Byte" -> return PsiType.BYTE
            "kotlin.Char" -> return PsiType.CHAR
            "kotlin.Double" -> return PsiType.DOUBLE
            "kotlin.Float" -> return PsiType.FLOAT
        }
    }

    when (kotlinFqName) {
        "kotlin.IntArray" -> return PsiType.INT.createArrayType()
        "kotlin.LongArray" -> return PsiType.LONG.createArrayType()
        "kotlin.ShortArray" -> return PsiType.SHORT.createArrayType()
        "kotlin.BooleanArray" -> return PsiType.BOOLEAN.createArrayType()
        "kotlin.ByteArray" -> return PsiType.BYTE.createArrayType()
        "kotlin.CharArray" -> return PsiType.CHAR.createArrayType()
        "kotlin.DoubleArray" -> return PsiType.DOUBLE.createArrayType()
        "kotlin.FloatArray" -> return PsiType.FLOAT.createArrayType()
    }

    val javaFqName = JavaToKotlinClassMap.mapKotlinToJava(FqNameUnsafe(kotlinFqName))?.asSingleFqName()?.asString() ?: kotlinFqName
    return PsiType.getTypeByName(javaFqName, context.project, context.resolveScope)
}

class KtLightPsiNameValuePair(
    override val kotlinOrigin: KtElement,
    private val name: String,
    lightParent: PsiElement,
    private val argument: (KtLightPsiNameValuePair) -> PsiAnnotationMemberValue?
) : KtLightElementBase(lightParent),
    PsiNameValuePair {

    override fun setValue(newValue: PsiAnnotationMemberValue): PsiAnnotationMemberValue = cannotModify()

    override fun getNameIdentifier(): PsiIdentifier? = LightIdentifier(kotlinOrigin.manager, name)

    override fun getName(): String? = name

    private val _value: PsiAnnotationMemberValue? by lazyPub { argument(this) }

    override fun getValue(): PsiAnnotationMemberValue? = _value

    override fun getLiteralValue(): String? = (value as? PsiLiteralExpression)?.value?.toString()

}
