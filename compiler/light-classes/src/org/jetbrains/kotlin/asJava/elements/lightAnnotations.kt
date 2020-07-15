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
import org.jetbrains.kotlin.asJava.fastCheckIsNullabilityApplied
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.nullability

private val LOG = Logger.getInstance("#org.jetbrains.kotlin.asJava.elements.lightAnnotations")

abstract class KtLightAbstractAnnotation(parent: PsiElement, computeDelegate: Lazy<PsiAnnotation>?) :
    KtLightElementBase(parent), PsiAnnotation, KtLightElement<KtCallElement, PsiAnnotation> {

    override val clsDelegate: PsiAnnotation by lazyPub {
        if (!accessAnnotationsClsDelegateIsAllowed && ApplicationManager.getApplication().isUnitTestMode && this !is KtLightNonSourceAnnotation)
            LOG.error("KtLightAbstractAnnotation clsDelegate requested for ${this.javaClass}")
        computeDelegate?.value ?: throw IllegalStateException("Cannot get class delegate for annotation light class")
    }

    override fun getNameReferenceElement() = clsDelegate.nameReferenceElement

    override fun getOwner(): PsiAnnotationOwner? = parent as? PsiAnnotationOwner

    override fun getParameterList(): PsiAnnotationParameterList = clsDelegate.parameterList

    open fun fqNameMatches(fqName: String): Boolean = qualifiedName == fqName
}

class KtLightAnnotationForSourceEntry(
    private val lazyQualifiedName: () -> String?,
    override val kotlinOrigin: KtCallElement,
    parent: PsiElement,
    private val lazyClsDelegate: Lazy<PsiAnnotation>?
) : KtLightAbstractAnnotation(parent, lazyClsDelegate) {

    private val _qualifiedName: String? by lazyPub { lazyQualifiedName() }

    override fun getOwner() = parent as? PsiAnnotationOwner

    override fun getQualifiedName(): String? = _qualifiedName

    override fun getName(): String? = null

    override fun findAttributeValue(name: String?) = getAttributeValue(name, true)

    override fun findDeclaredAttributeValue(name: String?): PsiAnnotationMemberValue? = getAttributeValue(name, false)

    private fun getCallEntry(name: String): MutableMap.MutableEntry<ValueParameterDescriptor, ResolvedValueArgument>? {
        val resolvedCall = kotlinOrigin.getResolvedCall() ?: return null
        return resolvedCall.valueArguments.entries.find { (param, _) -> param.name.asString() == name }
    }

    private fun getAttributeValue(name: String?, useDefault: Boolean): PsiAnnotationMemberValue? {

        ktLightAnnotationParameterList.attributes
            .find { it.name == (name ?: "value") }
            ?.let { return it.value }

        if (useDefault) {
            val callEntry = getCallEntry(name ?: "value") ?: return null

            if (callEntry.key.declaresOrInheritsDefaultValue()) {
                when (val psiElement = callEntry.key.source.getPsi()) {
                    is KtParameter ->
                        return psiElement.defaultValue?.let { convertToLightAnnotationMemberValue(this, it) }
                    is PsiAnnotationMethod ->
                        return psiElement.defaultValue
                }

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
        { lazyClsDelegate?.value?.nameReferenceElement }
    )

    private val ktLightAnnotationParameterList by lazyPub { KtLightAnnotationParameterList() }

    override fun getParameterList(): PsiAnnotationParameterList = ktLightAnnotationParameterList

    inner class KtLightAnnotationParameterList : KtLightElementBase(this),
        PsiAnnotationParameterList {
        override val kotlinOrigin: KtElement? get() = null

        private fun checkIfToArrayConversionExpected(callEntry: Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>): Boolean {

            if (!callEntry.key.isVararg) {
                return false
            }

            //Anno()
            val valueArgument = callEntry.value.arguments.firstOrNull() ?: return false

            //Anno(1,2,3)
            if (valueArgument is VarargValueArgument) {
                return true
            }

            //Anno(*[1,2,3])
            if (valueArgument is KtValueArgument && valueArgument.isSpread) {
                return false
            }

            //Anno(a = [1,2,3])
            return !valueArgument.isNamed()
        }

        private fun getWrappedToArrayNameValuePair(
            resolvedArgumentEntry: Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>
        ): KtLightPsiNameValuePair {

            val argumentExpressions =
                resolvedArgumentEntry.value.arguments.mapNotNull { varargArgument -> varargArgument.getArgumentExpression() }

            val parent = PsiTreeUtil.findCommonParent(argumentExpressions) as? KtElement
                ?: this@KtLightAnnotationForSourceEntry.kotlinOrigin.valueArgumentList
                ?: this@KtLightAnnotationForSourceEntry.kotlinOrigin

            val argumentName = resolvedArgumentEntry.key.name.asString()

            return KtLightPsiNameValuePair(
                parent,
                argumentName,
                this
            ) { self ->
                KtLightPsiArrayInitializerMemberValue(parent, self) { memberValue ->
                    argumentExpressions.map { argumentExpression ->
                        convertToLightAnnotationMemberValue(memberValue, argumentExpression)
                    }
                }
            }
        }

        private fun getNotWrappedToArrayNameValuePair(
            resolvedArgumentEntry: Map.Entry<ValueParameterDescriptor, ResolvedValueArgument>
        ): KtLightPsiNameValuePair? {

            val firstArgument = resolvedArgumentEntry.value.arguments.firstOrNull() ?: return null
            val argumentExpression = firstArgument.getArgumentExpression() ?: return null

            val argumentName = resolvedArgumentEntry.key.name.asString()

            return KtLightPsiNameValuePair(
                firstArgument.asElement(),
                argumentName,
                this
            ) { valuePair -> convertToLightAnnotationMemberValue(valuePair, argumentExpression) }
        }

        private val _attributes: Array<PsiNameValuePair> by lazyPub {

            if (this@KtLightAnnotationForSourceEntry.kotlinOrigin.valueArguments.isEmpty()) {
                return@lazyPub emptyArray()
            }

            val resolvedArguments =
                this@KtLightAnnotationForSourceEntry.kotlinOrigin.getResolvedCall()?.valueArguments

            resolvedArguments ?: return@lazyPub emptyArray()

            resolvedArguments.mapNotNull { resolvedArgumentEntry ->
                if (checkIfToArrayConversionExpected(resolvedArgumentEntry)) {
                    getWrappedToArrayNameValuePair(resolvedArgumentEntry)
                } else {
                    getNotWrappedToArrayNameValuePair(resolvedArgumentEntry)
                }
            }.toTypedArray()
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
) : KtLightAbstractAnnotation(parent, lazyPub { clsDelegate }) {
    override val kotlinOrigin: KtAnnotationEntry? get() = null
    override fun getQualifiedName() = kotlinOrigin?.name ?: clsDelegate.qualifiedName
    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()
    override fun findAttributeValue(attributeName: String?) = clsDelegate.findAttributeValue(attributeName)
    override fun findDeclaredAttributeValue(attributeName: String?) = clsDelegate.findDeclaredAttributeValue(attributeName)
}

class KtLightNonExistentAnnotation(parent: KtLightElement<*, *>) : KtLightElementBase(parent), PsiAnnotation {
    override val kotlinOrigin: KtElement? get() = null
    override fun toString(): String = this.javaClass.name

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement? = null
    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? = null
    override fun getQualifiedName(): String? = null
    override fun getOwner(): PsiAnnotationOwner? = parent as? PsiAnnotationOwner
    override fun findDeclaredAttributeValue(attributeName: String?): PsiAnnotationMemberValue? = null
    override fun getParameterList(): KtLightEmptyAnnotationParameterList = KtLightEmptyAnnotationParameterList(this)
}

class KtLightEmptyAnnotationParameterList(parent: PsiElement) : KtLightElementBase(parent), PsiAnnotationParameterList {
    override val kotlinOrigin: KtElement? get() = null
    override fun getAttributes(): Array<PsiNameValuePair> = emptyArray()
}

open class KtLightNullabilityAnnotation<D : KtLightElement<*, PsiModifierListOwner>>(val member: D, parent: PsiElement) :
    KtLightAbstractAnnotation(parent, lazyPub {
        // searching for last because nullability annotations are generated after backend generates source annotations
        getClsNullabilityAnnotation(member) ?: KtLightNonExistentAnnotation(member)
    }) {
    override fun fqNameMatches(fqName: String): Boolean {
        if (!isNullabilityAnnotation(fqName)) return false

        return super.fqNameMatches(fqName)
    }

    override val kotlinOrigin: Nothing? get() = null
    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? = null

    override fun getQualifiedName(): String? {
        val annotatedElement = member.takeIf(::isFromSources)?.kotlinOrigin
            ?: // it is out of our hands
            return getClsNullabilityAnnotation(member)?.qualifiedName

        if (!fastCheckIsNullabilityApplied(member)) return null

        // all data-class generated members are not-null
        if (annotatedElement is KtClass && annotatedElement.isData()) return NotNull::class.java.name

        // objects and companion objects are not null but ctor that does not annotated
        if (annotatedElement is KtObjectDeclaration) {
            if ((parent.parent as? PsiMethod)?.isConstructor == true) return null
            return NotNull::class.java.name
        }

        // don't annotate property setters
        if (annotatedElement is KtValVarKeywordOwner && member is KtLightMethod && member.returnType == PsiType.VOID) return null

        if (annotatedElement is KtNamedFunction && annotatedElement.modifierList?.hasSuspendModifier() == true) {
            return Nullable::class.java.name
        }

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

        return when (kotlinType.nullability()) {
            TypeNullability.NOT_NULL -> NotNull::class.java.name
            TypeNullability.NULLABLE -> Nullable::class.java.name
            TypeNullability.FLEXIBLE -> null
        }
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

        return annotatedElement.getParentOfType<KtProperty>(false)?.let {
            it.typeReference?.getType()
                ?: it.initializer
                    ?.let { initializer -> initializer.getType(initializer.analyze()) }
        }
    }

    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement? = null

    override fun getParameterList(): PsiAnnotationParameterList = KtLightEmptyAnnotationParameterList(this)

    override fun findDeclaredAttributeValue(attributeName: String?): PsiAnnotationMemberValue? = null
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
    @Suppress("NAME_SHADOWING") val argument = unwrapCall(argument)
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
                    { annotationName },
                    argument,
                    lightParent,
                    lazyClsDelegate = null
                )
            }
            val resolvedCall = argument.getResolvedCall()
            if (resolvedCall != null && CompileTimeConstantUtils.isArrayFunctionCall(resolvedCall))
                return KtLightPsiArrayInitializerMemberValue(
                    argument,
                    lightParent
                ) { self ->
                    arguments.mapNotNull {
                        it.getArgumentExpression()?.let { expression -> convertToLightAnnotationMemberValue(self, expression) }
                    }
                }
        }
        is KtCollectionLiteralExpression -> {
            val arguments = argument.getInnerExpressions()
            if (arguments.isNotEmpty())
                return KtLightPsiArrayInitializerMemberValue(
                    argument,
                    lightParent
                ) { self -> arguments.map { convertToLightAnnotationMemberValue(self, it) } }
        }
    }
    // everything else (like complex constant references) considered as PsiLiteral-s
    return KtLightPsiLiteral(argument, lightParent)
}

private val KtExpression.nameReference: KtNameReferenceExpression?
    get() = when (this) {
        is KtConstructorCalleeExpression -> constructorReferenceExpression as? KtNameReferenceExpression
        else -> this as? KtNameReferenceExpression
    }

private fun unwrapCall(callee: KtExpression): KtExpression = when (callee) {
    is KtDotQualifiedExpression -> callee.lastChild as? KtCallExpression ?: callee
    else -> callee
}

private fun getAnnotationName(callee: KtExpression): String? {
    @Suppress("NAME_SHADOWING") val callee = unwrapCall(callee)
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

@get:TestOnly
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
