/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightSimpleModifierList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.KotlinType

internal abstract class KtUltraLightParameter(
    name: String,
    override val kotlinOrigin: KtDeclaration?,
    protected val support: KtUltraLightSupport,
    method: KtLightMethod
) : org.jetbrains.kotlin.asJava.elements.LightParameter(
    name,
    PsiType.NULL,
    method,
    method.language
), KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiParameter> {

    override fun isEquivalentTo(another: PsiElement?): Boolean = kotlinOrigin == another

    override val clsDelegate: PsiParameter
        get() = throw IllegalStateException("Cls delegate shouldn't be loaded for ultra-light PSI!")

    private val lightModifierList by lazyPub { KtLightSimpleModifierList(this, emptySet()) }

    override fun getModifierList(): PsiModifierList = lightModifierList

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: method.navigationElement
    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: LocalSearchScope(this)

    override fun isValid() = parent.isValid

    protected abstract val kotlinType: KotlinType?
    protected abstract fun computeContainingDescriptor(): CallableDescriptor?

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
        val containingDescriptor = computeContainingDescriptor() ?: return@lazyPub PsiType.NULL
        support.mapType(this) { typeMapper, sw ->
            typeMapper.writeParameterType(sw, kotlinType, containingDescriptor)
        }
    }

    override fun getType(): PsiType = _type

    override fun getContainingFile(): PsiFile = method.containingFile
    override fun getParent(): PsiElement = method.parameterList

    override fun equals(other: Any?): Boolean =
        other is KtUltraLightParameter && other.kotlinOrigin == this.kotlinOrigin

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    abstract override fun isVarArgs(): Boolean
}

internal abstract class KtAbstractUltraLightParameterForDeclaration(
    name: String,
    kotlinOrigin: KtDeclaration?,
    support: KtUltraLightSupport,
    method: KtLightMethod,
    protected val containingDeclaration: KtCallableDeclaration
) : KtUltraLightParameter(name, kotlinOrigin, support, method) {
    override fun computeContainingDescriptor() = containingDeclaration.resolve() as? CallableMemberDescriptor
}

internal class KtUltraLightParameterForSource(
    name: String,
    override val kotlinOrigin: KtParameter,
    support: KtUltraLightSupport,
    method: KtLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, kotlinOrigin, support, method, containingDeclaration) {

    override val kotlinType: KotlinType? by lazyPub {
        kotlinOrigin.getKotlinType()
    }

    override fun isVarArgs(): Boolean = kotlinOrigin.isVarArg && method.parameterList.parameters.last() == this

    override fun setName(@NonNls name: String): PsiElement {
        kotlinOrigin.setName(name)
        return this
    }
}

internal class KtUltraLightParameterForSetterParameter(
    name: String,
    // KtProperty or KtParameter from primary constructor
    private val property: KtDeclaration,
    support: KtUltraLightSupport,
    method: KtLightMethod,
    containingDeclaration: KtCallableDeclaration
) : KtAbstractUltraLightParameterForDeclaration(name, null, support, method, containingDeclaration) {

    override val kotlinType: KotlinType? by lazyPub { property.getKotlinType() }

    override fun isVarArgs(): Boolean = false
}

internal class KtUltraLightReceiverParameter(
    containingDeclaration: KtCallableDeclaration,
    support: KtUltraLightSupport,
    method: KtLightMethod
) : KtAbstractUltraLightParameterForDeclaration("\$self", null, support, method, containingDeclaration) {

    override fun isVarArgs(): Boolean = false

    override val kotlinType: KotlinType? by lazyPub { computeContainingDescriptor()?.extensionReceiverParameter?.type }
}

internal class KtUltraLightParameterForDescriptor(
    private val descriptor: ParameterDescriptor,
    kotlinOrigin: KtDeclaration?,
    support: KtUltraLightSupport,
    method: KtLightMethod
) : KtUltraLightParameter(
    if (descriptor.name.isSpecial) "\$self" else descriptor.name.identifier,
    kotlinOrigin, support, method
) {
    override val kotlinType: KotlinType?
        get() = descriptor.type

    override fun computeContainingDescriptor() = descriptor.containingDeclaration as? CallableMemberDescriptor

    override fun isVarArgs() = (descriptor as? ValueParameterDescriptor)?.varargElementType != null

    override val givenAnnotations: List<KtLightAbstractAnnotation>
        get() = descriptor.obtainLightAnnotations(support, this)
}
