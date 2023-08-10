/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.Language
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightFieldBuilder
import com.intellij.psi.util.TypeConversionUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds.TRANSIENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.JvmStandardClassIds.VOLATILE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType

private class KtUltraLightFieldModifierList(
    support: KtUltraLightSupport,
    private val declaration: KtNamedDeclaration,
    owner: KtLightElement<KtModifierListOwner, PsiModifierListOwner>,
    private val modifiers: Set<String>,
) : KtUltraLightModifierList<KtLightElement<KtModifierListOwner, PsiModifierListOwner>>(owner, support) {

    override fun hasModifierProperty(name: String): Boolean = when (name) {
        PsiModifier.VOLATILE -> hasFieldAnnotation(VOLATILE_ANNOTATION_FQ_NAME)
        PsiModifier.TRANSIENT -> hasFieldAnnotation(TRANSIENT_ANNOTATION_FQ_NAME)
        else -> modifiers.contains(name)
    }

    private fun hasFieldAnnotation(fqName: FqName): Boolean {
        val annotation = support.findAnnotation(declaration, fqName)?.first ?: return false
        val target = annotation.useSiteTarget?.getAnnotationUseSiteTarget() ?: return true
        val expectedTarget =
            if (declaration is KtProperty && declaration.hasDelegate()) AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            else AnnotationUseSiteTarget.FIELD
        return target == expectedTarget
    }

    override fun copy() = KtUltraLightFieldModifierList(support, declaration, owner, modifiers)
}

internal class KtUltraLightFieldForSourceDeclaration(
    declaration: KtNamedDeclaration,
    name: String,
    containingClass: KtLightClass,
    support: KtUltraLightSupport,
    modifiers: Set<String>,
) : KtUltraLightFieldImpl(declaration, name, containingClass, support, modifiers),
    KtLightFieldForSourceDeclarationSupport {

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, declaration)
    override fun getStartOffsetInParent(): Int = kotlinOrigin.startOffsetInParent
    override fun isWritable(): Boolean = kotlinOrigin.isWritable
    override fun getNavigationElement(): PsiElement = kotlinOrigin.navigationElement ?: this
    override fun getContainingFile(): PsiFile = parent.containingFile
    override fun getPresentation(): ItemPresentation? = kotlinOrigin.let { ItemPresentationProviders.getItemPresentation(it) }
    override fun findElementAt(offset: Int): PsiElement? = kotlinOrigin.findElementAt(offset)

    // Workaround for KT-42137 until we update the bootstrap compiler.
    override val kotlinOrigin: KtNamedDeclaration get() = super.kotlinOrigin
}

internal open class KtUltraLightFieldImpl protected constructor(
    protected val declaration: KtNamedDeclaration,
    name: String,
    private val containingClass: KtLightClass,
    private val support: KtUltraLightSupport,
    modifiers: Set<String>,
) : LightFieldBuilder(name, PsiType.NULL, declaration), KtLightField,
    KtUltraLightElementWithNullabilityAnnotationDescriptorBased<KtDeclaration, PsiField> {

    private val modifierList by lazyPub {
        KtUltraLightFieldModifierList(support, declaration, this, modifiers)
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        kotlinOrigin == another || (another as? KtLightField)?.kotlinOrigin == kotlinOrigin

    override fun getModifierList(): PsiModifierList = modifierList

    override fun hasModifierProperty(name: String): Boolean =
        modifierList.hasModifierProperty(name) //can be removed after IDEA platform does the same

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun getText(): String? = kotlinOrigin.text
    override fun getTextRange(): TextRange? = kotlinOrigin.textRange
    override fun getTextOffset(): Int = kotlinOrigin.textOffset

    private val variableDescriptor: VariableDescriptor?
        get() = declaration.resolve()
            ?.let { it as? PropertyDescriptor ?: it as? ValueParameterDescriptor }

    private val kotlinType: KotlinType?
        get() = when {
            declaration is KtProperty && declaration.hasDelegate() ->
                declaration.delegateExpression?.let {
                    LightClassGenerationSupport.getInstance(project).analyze(it).getType(it)
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
        fun nonExistent() = JavaPsiFacade.getElementFactory(project).createTypeFromText(
            StandardNames.NON_EXISTENT_CLASS.asString(), declaration
        )

        when {
            (declaration is KtProperty && declaration.hasDelegate()) || declaration is KtEnumEntry || declaration is KtObjectDeclaration ->
                kotlinType?.asPsiType(support, TypeMappingMode.DEFAULT, this)
                    ?.let(TypeConversionUtil::erasure)
                    ?: nonExistent()

            else -> {
                val kotlinType = declaration.getKotlinType() ?: return@lazyPub PsiType.NULL
                val descriptor = variableDescriptor ?: return@lazyPub PsiType.NULL

                support.mapType(kotlinType, this) { typeMapper, sw ->
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

    override val lightMemberOrigin = LightMemberOriginForDeclaration(declaration, JvmDeclarationOriginKind.OTHER)

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    override fun setInitializer(initializer: PsiExpression?) = cannotModify()
}
