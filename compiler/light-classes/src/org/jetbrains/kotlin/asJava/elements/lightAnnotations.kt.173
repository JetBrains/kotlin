/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.nullability

private val LOG = Logger.getInstance("#org.jetbrains.kotlin.asJava.elements.lightAnnotations")

abstract class KtLightAbstractAnnotation(parent: PsiElement, computeDelegate: () -> PsiAnnotation) :
    KtLightElementBase(parent), PsiAnnotation, KtLightElement<KtCallElement, PsiAnnotation> {

    private val _clsDelegate: PsiAnnotation by lazyPub(computeDelegate)

    override val clsDelegate: PsiAnnotation
        get() {
            if (!accessAnnotationsClsDelegateIsAllowed && ApplicationManager.getApplication().isUnitTestMode && this !is KtLightNonSourceAnnotation)
                LOG.error("KtLightAbstractAnnotation clsDelegate requested for ${this.javaClass}")
            return _clsDelegate
        }

    override fun getNameReferenceElement() = clsDelegate.nameReferenceElement

    override fun getOwner() = parent as? PsiAnnotationOwner

    override fun getMetaData() = clsDelegate.metaData

    override fun getParameterList() = clsDelegate.parameterList

    override fun canNavigate(): Boolean = super<KtLightElementBase>.canNavigate()

    override fun canNavigateToSource(): Boolean = super<KtLightElementBase>.canNavigateToSource()

    override fun navigate(requestFocus: Boolean) = super<KtLightElementBase>.navigate(requestFocus)

    open fun fqNameMatches(fqName: String): Boolean = qualifiedName == fqName
}

class KtLightAnnotationForSourceEntry(
        private val qualifiedName: String,
        override val kotlinOrigin: KtCallElement,
        parent: PsiElement,
        computeDelegate: () -> PsiAnnotation
) : KtLightAbstractAnnotation(parent, computeDelegate) {

    override fun getQualifiedName() = qualifiedName

    override fun isPhysical() = true

    override fun getName(): String? = null

    override fun findAttributeValue(name: String?) = getAttributeValue(name, true)

    override fun findDeclaredAttributeValue(name: String?): PsiAnnotationMemberValue? = getAttributeValue(name, false)

    private fun getCallEntry(name: String): MutableMap.MutableEntry<ValueParameterDescriptor, ResolvedValueArgument>? {
        val resolvedCall = kotlinOrigin.getResolvedCall() ?: return null
        return resolvedCall.valueArguments.entries.find { (param, _) -> param.name.asString() == name } ?: return null
    }

    private fun getAttributeValue(name: String?, useDefault: Boolean): PsiAnnotationMemberValue? {
        val name = name ?: "value"
        val callEntry = getCallEntry(name) ?: return null

        val valueArgument = callEntry.value.arguments.firstOrNull()
        if (valueArgument != null) {
            ktLightAnnotationParameterList.attributes.find { (it as KtLightPsiNameValuePair).valueArgument === valueArgument }?.let {
                return it.value
            }
        }

        if (useDefault && callEntry.key.declaresOrInheritsDefaultValue()) {
            val psiElement = callEntry.key.source.getPsi()
            when (psiElement) {
                is KtParameter ->
                    return psiElement.defaultValue?.let { convertToLightAnnotationMemberValue(this, it) }
                is PsiAnnotationMethod ->
                    return psiElement.defaultValue
            }
        }
        return null
    }


    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement? = KtLightPsiJavaCodeReferenceElement(
        kotlinOrigin.navigationElement,
        {
            (kotlinOrigin as? KtAnnotationEntry)?.typeReference?.reference
                ?: (kotlinOrigin.calleeExpression?.nameReference)?.references?.firstOrNull()
        },
        { super.getNameReferenceElement() }
    )


    private val ktLightAnnotationParameterList by lazyPub { KtLightAnnotationParameterList() }

    override fun getParameterList(): PsiAnnotationParameterList = ktLightAnnotationParameterList

    inner class KtLightAnnotationParameterList() : KtLightElementBase(this),
        PsiAnnotationParameterList {
        override val kotlinOrigin: KtElement? get() = null

        private val _attributes: Array<PsiNameValuePair> by lazyPub {
            this@KtLightAnnotationForSourceEntry.kotlinOrigin.valueArguments.map { makeLightPsiNameValuePair(it as KtValueArgument) }
                .toTypedArray<PsiNameValuePair>()
        }

        private fun makeArrayInitializerIfExpected(pair: KtLightPsiNameValuePair): PsiAnnotationMemberValue? {
            val valueArgument = pair.valueArgument
            val name = valueArgument.name ?: "value"
            val callEntry = getCallEntry(name) ?: return null

            val valueArguments = callEntry.value.arguments
            val argument = valueArguments.firstOrNull()?.getArgumentExpression() ?: return null

            if (!callEntry.key.type.let { KotlinBuiltIns.isArray(it) }) return null

            if (argument !is KtStringTemplateExpression && argument !is KtConstantExpression && getAnnotationName(argument) == null) {
                return null
            }

            val parent = PsiTreeUtil.findCommonParent(valueArguments.map { it.getArgumentExpression() }) as KtElement
            return KtLightPsiArrayInitializerMemberValue(parent, pair) { self ->
                valueArguments.mapNotNull {
                    it.getArgumentExpression()?.let { convertToLightAnnotationMemberValue(self, it) }
                }
            }
        }

        private fun makeLightPsiNameValuePair(valueArgument: KtValueArgument) = KtLightPsiNameValuePair(valueArgument, this) { self ->
            makeArrayInitializerIfExpected(self)
                ?: self.valueArgument.getArgumentExpression()?.let { convertToLightAnnotationMemberValue(self, it) }
        }

        override fun getAttributes(): Array<PsiNameValuePair> = _attributes

    }


    override fun delete() = kotlinOrigin.delete()

    override fun toString() = "@$qualifiedName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false
        return kotlinOrigin == (other as KtLightAnnotationForSourceEntry).kotlinOrigin
    }

    override fun hashCode() = kotlinOrigin.hashCode()

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()
}

class KtLightNonSourceAnnotation(
    parent: PsiElement, clsDelegate: PsiAnnotation
) : KtLightAbstractAnnotation(parent, { clsDelegate }) {
    override val kotlinOrigin: KtAnnotationEntry? get() = null
    override fun getQualifiedName() = kotlinOrigin?.name ?: clsDelegate.qualifiedName
    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()
    override fun findAttributeValue(attributeName: String?) = clsDelegate.findAttributeValue(attributeName)
    override fun findDeclaredAttributeValue(attributeName: String?) = clsDelegate.findDeclaredAttributeValue(attributeName)
}

class KtLightNonExistentAnnotation(parent: KtLightElement<*, *>) : KtLightElementBase(parent), PsiAnnotation {
    override val kotlinOrigin get() = null
    override fun toString() = this.javaClass.name

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun getNameReferenceElement() = null
    override fun findAttributeValue(attributeName: String?) = null
    override fun getQualifiedName() = null
    override fun getOwner() = parent as? PsiAnnotationOwner
    override fun findDeclaredAttributeValue(attributeName: String?) = null
    override fun getMetaData() = null
    override fun getParameterList() = KtLightEmptyAnnotationParameterList(this)

    override fun canNavigate(): Boolean = super<KtLightElementBase>.canNavigate()

    override fun canNavigateToSource(): Boolean = super<KtLightElementBase>.canNavigateToSource()

    override fun navigate(requestFocus: Boolean) = super<KtLightElementBase>.navigate(requestFocus)
}

class KtLightEmptyAnnotationParameterList(parent: PsiElement) : KtLightElementBase(parent), PsiAnnotationParameterList {
    override val kotlinOrigin get() = null
    override fun getAttributes(): Array<PsiNameValuePair> = emptyArray()
}

class KtLightNullabilityAnnotation(val member: KtLightElement<*, PsiModifierListOwner>, parent: PsiElement) :
    KtLightAbstractAnnotation(parent, {
    // searching for last because nullability annotations are generated after backend generates source annotations
        getClsNullabilityAnnotation(member) ?: KtLightNonExistentAnnotation(member)
}) {
    override fun fqNameMatches(fqName: String): Boolean {
        if (!isNullabilityAnnotation(fqName)) return false

        return super.fqNameMatches(fqName)
    }

    override val kotlinOrigin get() = null
    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun findAttributeValue(attributeName: String?) = null

    override fun getQualifiedName(): String? {
        val annotatedElement = member.takeIf(::isFromSources)?.kotlinOrigin
                ?: // it is out of our hands
                return getClsNullabilityAnnotation(member)?.qualifiedName

        // all data-class generated members are not-null
        if (annotatedElement is KtClass && annotatedElement.isData()) return NotNull::class.java.name

        if (annotatedElement is KtParameter) {
            if (annotatedElement.containingClassOrObject?.isAnnotation() == true) return null
            if (isNullableInJvmOverloads(annotatedElement)) return Nullable::class.java.name
        }

        // don't annotate property setters
        if (annotatedElement is KtValVarKeywordOwner && member is KtLightMethod && member.returnType == PsiType.VOID) return null

        val kotlinType = getTargetType(annotatedElement) ?: return null
        if (KotlinBuiltIns.isPrimitiveType(kotlinType) && (annotatedElement as? KtParameter)?.isVarArg != true) {
            // no need to annotate them explicitly except the case when overriding reference-type makes it non-primitive for Jvm
            if (!(annotatedElement is KtCallableDeclaration && annotatedElement.hasModifier(KtTokens.OVERRIDE_KEYWORD))) return null

            val overriddenDescriptors =
                (annotatedElement.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, annotatedElement] as? CallableMemberDescriptor)?.overriddenDescriptors
            if (overriddenDescriptors?.all { it.returnType == kotlinType } == true) return null
        }
        if (kotlinType.isUnit() && (annotatedElement !is KtValVarKeywordOwner)) return null // not annotate unit-functions
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

    private fun isNullableInJvmOverloads(annotatedElement: KtParameter): Boolean {
        if (annotatedElement.ownerFunction?.let { it.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, it]?.findJvmOverloadsAnnotation() } == null) return false
        val lightParameterList = (member as? PsiParameter)?.parent as? PsiParameterList ?: return false
        val lastParameter = (lightParameterList.parameters.lastOrNull() as? KtLightElement<*, *>)?.kotlinOrigin
        return lastParameter == annotatedElement
    }

    internal fun KtTypeReference.getType(): KotlinType? = analyze()[BindingContext.TYPE, this]

    private fun getTargetType(annotatedElement: PsiElement): KotlinType? {
        if (annotatedElement is KtTypeReference) {
            annotatedElement.getType()?.let { return it }
        }
        if (annotatedElement is KtCallableDeclaration) {
            annotatedElement.typeReference?.getType()?.let { return it }
        }
        if (annotatedElement is KtNamedFunction) {
            annotatedElement.bodyExpression?.let { it.getType(it.analyze()) }?.let { return it }
        }
        if (annotatedElement is KtProperty) {
            annotatedElement.initializer?.let { it.getType(it.analyze()) }?.let { return it }
            annotatedElement.delegateExpression?.let { it.getType(it.analyze())?.arguments?.firstOrNull()?.type }?.let { return it }
        }
        annotatedElement.getParentOfType<KtProperty>(false)?.let {
            it.typeReference?.getType() ?: it.initializer?.let { it.getType(it.analyze()) }
        }?.let { return it }
        return null
    }


    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement? = null

    override fun getParameterList(): PsiAnnotationParameterList = KtLightEmptyAnnotationParameterList(this)

    override fun findDeclaredAttributeValue(attributeName: String?) = null
}

private fun getClsNullabilityAnnotation(member: KtLightElement<*, PsiModifierListOwner>): PsiAnnotation? {
    if (!accessAnnotationsClsDelegateIsAllowed && ApplicationManager.getApplication().isUnitTestMode && isFromSources(member) && member.kotlinOrigin != null)
        LOG.error("nullability should be retrieved from `kotlinOrigin`")
    return member.clsDelegate.modifierList?.annotations?.findLast {
        isNullabilityAnnotation(it.qualifiedName)
    }
}

internal fun isNullabilityAnnotation(qualifiedName: String?) = qualifiedName in backendNullabilityAnnotations

private val backendNullabilityAnnotations = arrayOf(Nullable::class.java.name, NotNull::class.java.name)

private fun KtElement.analyze(): BindingContext = LightClassGenerationSupport.getInstance(this.project).analyze(this)

private fun KtElement.getResolvedCall(): ResolvedCall<out CallableDescriptor>? {
    if (!isValid) return null
    val context = analyze()
    return this.getResolvedCall(context)
}

fun convertToLightAnnotationMemberValue(lightParent: PsiElement, argument: KtExpression): PsiAnnotationMemberValue {
    val argument = unwrapCall(argument)
    when (argument) {
        is KtClassLiteralExpression -> {
            return KtLightPsiClassObjectAccessExpression(argument, lightParent)
        }
        is KtStringTemplateExpression, is KtConstantExpression -> {
            return KtLightPsiLiteral(argument, lightParent)
        }
        is KtCallExpression -> {
            val arguments = argument.valueArguments
            val annotationName = argument.calleeExpression?.let { getAnnotationName(it) }
            if (annotationName != null) {
                return KtLightAnnotationForSourceEntry(
                    annotationName,
                    argument,
                    lightParent,
                    { throw UnsupportedOperationException("cls delegate is not supported for nested annotations") })
            }
            val resolvedCall = argument.getResolvedCall()
            if (resolvedCall != null && CompileTimeConstantUtils.isArrayFunctionCall(resolvedCall))
                return KtLightPsiArrayInitializerMemberValue(
                    argument,
                    lightParent,
                    { self ->
                        arguments.mapNotNull {
                            it.getArgumentExpression()?.let { convertToLightAnnotationMemberValue(self, it) }
                        }
                    })
        }
        is KtCollectionLiteralExpression -> {
            val arguments = argument.getInnerExpressions()
            if (arguments.isNotEmpty())
                return KtLightPsiArrayInitializerMemberValue(
                    argument,
                    lightParent,
                    { self -> arguments.mapNotNull { convertToLightAnnotationMemberValue(self, it) } })
        }
    }
    // everything else (like complex constant references) considered as PsiLiteral-s
    return KtLightPsiLiteral(argument, lightParent)
}

private val KtExpression.nameReference: KtNameReferenceExpression?
    get() = when {
        this is KtConstructorCalleeExpression -> constructorReferenceExpression as? KtNameReferenceExpression
        else -> this as? KtNameReferenceExpression
    }

private fun unwrapCall(callee: KtExpression): KtExpression = when (callee) {
    is KtDotQualifiedExpression -> callee.lastChild as? KtCallExpression ?: callee
    else -> callee
}

private fun getAnnotationName(callee: KtExpression): String? {
    val callee = unwrapCall(callee)
    val resultingDescriptor = callee.getResolvedCall()?.resultingDescriptor
    if (resultingDescriptor is ClassConstructorDescriptor) {
        val ktClass = resultingDescriptor.constructedClass.source.getPsi() as? KtClass
        if (ktClass?.isAnnotation() == true) return ktClass.fqName?.toString()
    }
    if (resultingDescriptor is JavaClassConstructorDescriptor) {
        val psiClass = resultingDescriptor.constructedClass.source.getPsi() as? PsiClass
        if (psiClass?.isAnnotationType == true) return psiClass.qualifiedName
    }
    return null
}

@TestOnly
var accessAnnotationsClsDelegateIsAllowed = false

@TestOnly
fun <T> withAllowedAnnotationsClsDelegate(body: () -> T): T {
    val prev = accessAnnotationsClsDelegateIsAllowed
    try {
        accessAnnotationsClsDelegateIsAllowed = true
        return body()
    } finally {
        accessAnnotationsClsDelegateIsAllowed = prev
    }
}