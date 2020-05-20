/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightFieldBuilder
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldForSourceDeclarationSupport
import org.jetbrains.kotlin.asJava.elements.KtUltraLightSimpleModifierList
import org.jetbrains.kotlin.codegen.PropertyCodegen
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.annotations.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.annotations.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType

private class KtUltraLightSimpleModifierListField(
    private val support: KtUltraLightSupport,
    private val declaration: KtNamedDeclaration,
    owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>,
    private val modifiers: Set<String>,
) : KtUltraLightSimpleModifierList(owner, modifiers, support) {
    override fun hasModifierProperty(name: String): Boolean = when (name) {
        PsiModifier.VOLATILE -> hasFieldAnnotation(VOLATILE_ANNOTATION_FQ_NAME)
        PsiModifier.TRANSIENT -> hasFieldAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)
        else -> super.hasModifierProperty(name)
    }

    private fun hasFieldAnnotation(fqName: FqName): Boolean {
        val annotation = support.findAnnotation(declaration, fqName)?.first ?: return false
        val target = annotation.useSiteTarget?.getAnnotationUseSiteTarget() ?: return true
        val expectedTarget =
            if (declaration is KtProperty && declaration.hasDelegate()) AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            else AnnotationUseSiteTarget.FIELD
        return target == expectedTarget
    }

    override fun copy() = KtUltraLightSimpleModifierListField(support, declaration, owner, modifiers)
}

internal class KtUltraLightFieldForSourceDeclaration(
    declaration: KtNamedDeclaration,
    name: String,
    containingClass: KtLightClass,
    support: KtUltraLightSupport,
    modifiers: Set<String>,
) : KtUltraLightFieldImpl(declaration, name, containingClass, support, modifiers),
    KtLightFieldForSourceDeclarationSupport

internal open class KtUltraLightFieldImpl protected constructor(
    protected val declaration: KtNamedDeclaration,
    name: String,
    private val containingClass: KtLightClass,
    private val support: KtUltraLightSupport,
    modifiers: Set<String>,
) : LightFieldBuilder(name, PsiType.NULL, declaration), KtLightField,
    KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiField> {

    private val modifierList by lazyPub {
        KtUltraLightSimpleModifierListField(support, declaration, this, modifiers)
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        kotlinOrigin == another || (another as? KtLightField)?.kotlinOrigin == kotlinOrigin

    override fun getModifierList(): PsiModifierList = modifierList

    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name) //can be removed after IDEA platform does the same

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    private val variableDescriptor: VariableDescriptor?
        get() = declaration.resolve()
            ?.let { it as? PropertyDescriptor ?: it as? ValueParameterDescriptor }

    private val kotlinType: KotlinType?
        get() = when {
            declaration is KtProperty && declaration.hasDelegate() ->
                (variableDescriptor as? PropertyDescriptor)?.let {
                    val context = LightClassGenerationSupport.getInstance(project).analyze(declaration)
                    PropertyCodegen.getDelegateTypeForProperty(it, context)
                }
            declaration is KtObjectDeclaration ->
                (declaration.resolve() as? ClassDescriptor)?.defaultType
            declaration is KtEnumEntry -> {
                (containingClass.kotlinOrigin?.resolve() as? ClassDescriptor)?.defaultType
            }
            else -> {
                declaration.getKotlinType()
            }
        }

    override val qualifiedNameForNullabilityAnnotation: String?
        get() =
            when (declaration) {
                is KtObjectDeclaration -> NotNull::class.java.name
                is KtEnumEntry -> null
                else -> computeQualifiedNameForNullabilityAnnotation(kotlinType)
            }

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = type

    private val _type: PsiType by lazyPub {
        fun nonExistent() = JavaPsiFacade.getElementFactory(project).createTypeFromText("error.NonExistentClass", declaration)

        when {
            (declaration is KtProperty && declaration.hasDelegate()) || declaration is KtEnumEntry || declaration is KtObjectDeclaration ->
                kotlinType?.asPsiType(support, TypeMappingMode.DEFAULT, this)
                    ?.let(TypeConversionUtil::erasure)
                    ?: nonExistent()
            else -> {
                val kotlinType = declaration.getKotlinType() ?: return@lazyPub PsiType.NULL
                val descriptor = variableDescriptor ?: return@lazyPub PsiType.NULL

                support.mapType(this) { typeMapper, sw ->
                    typeMapper.writeFieldSignature(kotlinType, descriptor, sw)
                }
            }
        }
    }

    override fun getType(): PsiType = _type
    override fun getParent() = containingClass
    override fun getContainingClass() = containingClass
    override fun getContainingFile(): PsiFile? = containingClass.containingFile

    private val _initializer by lazyPub {
        _constantInitializer?.createPsiLiteral(declaration)
    }

    override fun getInitializer(): PsiExpression? = _initializer

    override fun hasInitializer(): Boolean = initializer !== null

    private val _constantInitializer by lazyPub {
        if (declaration !is KtProperty) return@lazyPub null
        if (!declaration.hasModifier(KtTokens.CONST_KEYWORD)) return@lazyPub null
        if (!declaration.hasInitializer()) return@lazyPub null
        if (!hasModifierProperty(PsiModifier.FINAL)) return@lazyPub null
        if (!TypeConversionUtil.isPrimitiveAndNotNull(_type) && !_type.equalsToText(CommonClassNames.JAVA_LANG_STRING)) return@lazyPub null
        variableDescriptor?.compileTimeInitializer
    }

    override fun computeConstantValue(): Any? = _constantInitializer?.value

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? = computeConstantValue()

    override val kotlinOrigin = declaration

    override val clsDelegate: PsiField get() = invalidAccess()

    override val lightMemberOrigin = LightMemberOriginForDeclaration(declaration, JvmDeclarationOriginKind.OTHER)

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    override fun setInitializer(initializer: PsiExpression?) = cannotModify()
}