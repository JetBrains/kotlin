/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.LanguageConstantExpressionEvaluator
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightTypeElement
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.computeExpression
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class KtLightPsiArrayInitializerMemberValue(
    override val kotlinOrigin: KtElement,
    private val lightParent: PsiElement,
    private val arguments: (KtLightPsiArrayInitializerMemberValue) -> List<PsiAnnotationMemberValue>
) : KtLightElementBase(lightParent), PsiArrayInitializerMemberValue {
    override fun getInitializers(): Array<PsiAnnotationMemberValue> = arguments(this).toTypedArray()

    override fun getParent(): PsiElement = lightParent

    override fun isPhysical(): Boolean = false
}

open class KtLightPsiLiteral(
    override val kotlinOrigin: KtExpression,
    private val lightParent: PsiElement
) : KtLightElementBase(lightParent), PsiLiteralExpression {

    override fun getValue(): Any? = computeExpression(this)

    override fun getType(): PsiType? {
        val bindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(kotlinOrigin)
        val kotlinType = bindingContext[BindingContext.EXPECTED_EXPRESSION_TYPE, kotlinOrigin] ?: return null
        val typeFqName = kotlinType.constructor.declarationDescriptor?.fqNameSafe?.asString() ?: return null
        return psiType(typeFqName, kotlinOrigin)
    }

    override fun getParent(): PsiElement = lightParent

    override fun isPhysical(): Boolean = false

    override fun replace(newElement: PsiElement): PsiElement {
        val value = (newElement as? PsiLiteral)?.value as? String ?: return this
        kotlinOrigin.replace(KtPsiFactory(this).createExpression("\"${StringUtil.escapeStringCharacters(value)}\""))
        return this
    }

    override fun getReference(): PsiReference? = references.singleOrNull()
    override fun getReferences(): Array<out PsiReference> = kotlinOrigin.references
}

class KtLightPsiClassObjectAccessExpression(override val kotlinOrigin: KtClassLiteralExpression, lightParent: PsiElement) :
    KtLightPsiLiteral(kotlinOrigin, lightParent), PsiClassObjectAccessExpression {
    override fun getType(): PsiType {
        val bindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(kotlinOrigin)
        val (classId, arrayDimensions) = bindingContext[BindingContext.COMPILE_TIME_VALUE, kotlinOrigin]
            ?.toConstantValue(TypeUtils.NO_EXPECTED_TYPE)?.safeAs<KClassValue>()?.value
            ?.safeAs<KClassValue.Value.NormalClass>()?.value ?: return PsiType.VOID
        var type = psiType(classId.asSingleFqName().asString(), kotlinOrigin, boxPrimitiveType = arrayDimensions > 0)
        repeat(arrayDimensions) {
            type = type.createArrayType()
        }
        return type
    }

    override fun getOperand(): PsiTypeElement = LightTypeElement(kotlinOrigin.manager, type)
}

internal fun psiType(kotlinFqName: String, context: PsiElement, boxPrimitiveType: Boolean = false): PsiType {
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
