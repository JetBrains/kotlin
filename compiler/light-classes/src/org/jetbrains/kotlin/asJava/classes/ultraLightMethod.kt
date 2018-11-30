/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.asJava.elements.KtLightSimpleModifierList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType

internal class KtUltraLightMethod(
    internal val delegate: LightMethodBuilder,
    originalElement: KtDeclaration,
    private val support: UltraLightSupport,
    containingClass: KtUltraLightClass
) : KtLightMethodImpl(
    { delegate },
    LightMemberOriginForDeclaration(
        originalElement, JvmDeclarationOriginKind.OTHER
    ),
    containingClass
), KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiMethod> {

    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() = kotlinOrigin?.getKotlinType()

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = returnType

    // These two overrides are necessary because ones from KtLightMethodImpl suppose that clsDelegate.returnTypeElement is valid
    // While here we only set return type for LightMethodBuilder (see org.jetbrains.kotlin.asJava.classes.KtUltraLightClass.asJavaMethod)
    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun getReturnType(): PsiType? = clsDelegate.returnType

    override fun buildParametersForList(): List<PsiParameter> = clsDelegate.parameterList.parameters.toList()

    // should be in super
    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    override fun buildTypeParameterList(): PsiTypeParameterList {
        val origin = kotlinOrigin
        return if (origin is KtFunction || origin is KtProperty)
            buildTypeParameterList(origin as KtTypeParameterListOwner, this, support)
        else LightTypeParameterListBuilder(manager, language)
    }

    private val _throwsList: PsiReferenceList by lazyPub {
        val list = KotlinLightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST)
        (kotlinOrigin?.resolve() as? FunctionDescriptor)?.let {
            for (ex in FunctionCodegen.getThrownExceptions(it)) {
                list.addReference(ex.fqNameSafe.asString())
            }
        }
        list
    }

    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    override fun getThrowsList(): PsiReferenceList = _throwsList
}

internal class KtUltraLightParameter(
    name: String,
    override val kotlinOrigin: KtDeclaration,
    private val support: UltraLightSupport,
    method: KtLightMethod,
    private val receiver: KtTypeReference?,
    private val containingFunction: KtCallableDeclaration
) : org.jetbrains.kotlin.asJava.elements.LightParameter(
    name,
    PsiType.NULL,
    method,
    method.language
), KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiParameter> {

    override val clsDelegate: PsiParameter
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")

    private val lightModifierList by lazyPub { KtLightSimpleModifierList(this, emptySet()) }

    override fun isVarArgs(): Boolean =
        kotlinOrigin is KtParameter && kotlinOrigin.isVarArg && method.parameterList.parameters.last() == this

    override fun getModifierList(): PsiModifierList = lightModifierList

    override fun getNavigationElement(): PsiElement = kotlinOrigin

    override fun isValid() = parent.isValid

    private val kotlinType: KotlinType? by lazyPub {
        when {
            receiver != null -> (kotlinOrigin.resolve() as? CallableMemberDescriptor)?.extensionReceiverParameter?.type
            else -> kotlinOrigin.getKotlinType()
        }
    }

    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() {
            val type = kotlinType
            return if (isVarArgs && type != null && KotlinBuiltIns.isArray(type)) {
                type.arguments[0].type
            } else {
                type
            }
        }

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = type


    private val _type: PsiType by lazyPub {
        val kotlinType = kotlinType ?: return@lazyPub PsiType.NULL
        val containingDescriptor = containingFunction.resolve() as? CallableDescriptor ?: return@lazyPub PsiType.NULL
        support.mapType(this) { typeMapper, sw ->
            typeMapper.writeParameterType(sw, kotlinType, containingDescriptor)
        }
    }

    override fun getType(): PsiType = _type

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtVariableDeclaration)?.setName(name)
        return this
    }

    override fun getContainingFile(): PsiFile = method.containingFile
    override fun getParent(): PsiElement = method.parameterList

    override fun equals(other: Any?): Boolean = other is KtUltraLightParameter && other.kotlinOrigin == this.kotlinOrigin
    override fun hashCode(): Int = kotlinOrigin.hashCode()

    internal fun annotatedOrigin(): KtAnnotated? {
        if (receiver != null) return receiver

        if (kotlinOrigin is KtProperty) {
            return null // we're a setter of a property with no explicit declaration, so we don't have annotation
        }
        return kotlinOrigin
    }

    internal fun getTypeForNullability(): KotlinType? {
        if (receiver != null) return kotlinType
        if (kotlinOrigin is KtProperty) {
            if (kotlinOrigin.setter?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true) return null
            return kotlinType
        }
        if (kotlinOrigin is KtParameter) {
            val reference = kotlinOrigin.typeReference
            if (kotlinOrigin.isVarArg && reference != null) {
                LightClassGenerationSupport.getInstance(project).analyze(reference)[BindingContext.TYPE, reference]?.let { return it }
            }
            if (reference != null || kotlinOrigin.parent?.parent is KtPropertyAccessor) {
                return kotlinType
            }
        }
        return null
    }

}
