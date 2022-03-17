/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMapper
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.psiUtil.checkReservedYield
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.hasSuspendModifier
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.PossiblyBareType.bare
import org.jetbrains.kotlin.resolve.PossiblyBareType.type
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.calls.checkers.checkCoroutinesFeature
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.checkers.TrailingCommaChecker
import org.jetbrains.kotlin.resolve.descriptorUtil.findImplicitOuterClassArguments
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.findFirstFromMeAndParent
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.error.ErrorScope
import org.jetbrains.kotlin.types.error.ThrowingScope
import org.jetbrains.kotlin.types.extensions.TypeAttributeTranslators
import org.jetbrains.kotlin.types.typeUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.math.min

class TypeResolver(
    private val annotationResolver: AnnotationResolver,
    private val qualifiedExpressionResolver: QualifiedExpressionResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val typeTransformerForTests: TypeTransformerForTests,
    private val dynamicTypesSettings: DynamicTypesSettings,
    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val identifierChecker: IdentifierChecker,
    private val platformToKotlinClassMapper: PlatformToKotlinClassMapper,
    private val languageVersionSettings: LanguageVersionSettings,
    private val upperBoundChecker: UpperBoundChecker,
    private val typeAttributeTranslators: TypeAttributeTranslators
) {
    private val isNonParenthesizedAnnotationsOnFunctionalTypesEnabled =
        languageVersionSettings.getFeatureSupport(LanguageFeature.NonParenthesizedAnnotationsOnFunctionalTypes) == LanguageFeature.State.ENABLED

    open class TypeTransformerForTests {
        open fun transformType(kotlinType: KotlinType): KotlinType? = null
    }

    fun resolveType(scope: LexicalScope, typeReference: KtTypeReference, trace: BindingTrace, checkBounds: Boolean): KotlinType {
        // bare types are not allowed
        return resolveType(
            TypeResolutionContext(scope, trace, checkBounds, false, typeReference.suppressDiagnosticsInDebugMode(), false),
            typeReference
        )
    }

    fun resolveAbbreviatedType(scope: LexicalScope, typeReference: KtTypeReference, trace: BindingTrace): SimpleType {
        val resolvedType = resolveType(
            TypeResolutionContext(scope, trace, true, false, typeReference.suppressDiagnosticsInDebugMode(), true),
            typeReference
        ).unwrap()
        return when (resolvedType) {
            is DynamicType -> {
                trace.report(TYPEALIAS_SHOULD_EXPAND_TO_CLASS.on(typeReference, resolvedType))
                ErrorUtils.createErrorType(ErrorTypeKind.PROHIBITED_DYNAMIC_TYPE)
            }
            is SimpleType -> resolvedType
            else -> error("Unexpected type: $resolvedType")
        }
    }

    fun resolveExpandedTypeForTypeAlias(typeAliasDescriptor: TypeAliasDescriptor): SimpleType {
        val typeAliasExpansion = TypeAliasExpansion.createWithFormalArguments(typeAliasDescriptor)
        val expandedType = TypeAliasExpander.NON_REPORTING.expandWithoutAbbreviation(typeAliasExpansion, TypeAttributes.Empty)
        return expandedType
    }

    private fun resolveType(c: TypeResolutionContext, typeReference: KtTypeReference): KotlinType {
        assert(!c.allowBareTypes) { "Use resolvePossiblyBareType() when bare types are allowed" }
        return resolvePossiblyBareType(c, typeReference).actualType
    }

    fun resolvePossiblyBareType(c: TypeResolutionContext, typeReference: KtTypeReference): PossiblyBareType {
        val cachedType = c.trace.bindingContext.get(BindingContext.TYPE, typeReference)
        if (cachedType != null) return type(cachedType)

        val resolvedTypeSlice = if (c.abbreviated) BindingContext.ABBREVIATED_TYPE else BindingContext.TYPE

        val annotations = resolveTypeAnnotations(c.trace, c.scope, typeReference)
        val type = resolveTypeElement(c, annotations, typeReference.modifierList, typeReference.typeElement)
        c.trace.recordScope(c.scope, typeReference)

        if (!type.isBare) {
            for (argument in type.actualType.arguments) {
                forceResolveTypeContents(argument.type)
            }
            c.trace.record(resolvedTypeSlice, typeReference, type.actualType)
        }
        return type
    }

    internal fun KtElementImplStub<*>.getAllModifierLists(): Array<out KtDeclarationModifierList> =
        getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)

    // TODO: remove this method and its usages in 1.4
    private fun checkNonParenthesizedAnnotationsOnFunctionalType(
        typeElement: KtFunctionType,
        annotationEntries: List<KtAnnotationEntry>,
        trace: BindingTrace
    ) {
        val lastAnnotationEntry = annotationEntries.lastOrNull()
        val isAnnotationsGroupedUsingBrackets =
            lastAnnotationEntry?.getNextSiblingIgnoringWhitespaceAndComments()?.node?.elementType == KtTokens.RBRACKET
        val hasAnnotationParentheses = lastAnnotationEntry?.valueArgumentList != null
        val isFunctionalTypeStartingWithParentheses = typeElement.firstChild is KtParameterList
        val hasSuspendModifierBeforeParentheses =
            typeElement.getPrevSiblingIgnoringWhitespaceAndComments().run { this is KtDeclarationModifierList && hasSuspendModifier() }

        if (lastAnnotationEntry != null &&
            isFunctionalTypeStartingWithParentheses &&
            !hasAnnotationParentheses &&
            !isAnnotationsGroupedUsingBrackets &&
            !hasSuspendModifierBeforeParentheses
        ) {
            trace.report(Errors.NON_PARENTHESIZED_ANNOTATIONS_ON_FUNCTIONAL_TYPES.on(lastAnnotationEntry))
        }
    }

    fun resolveTypeAnnotations(trace: BindingTrace, scope: LexicalScope, modifierListsOwner: KtElementImplStub<*>): Annotations {
        val modifierLists = modifierListsOwner.getAllModifierLists()

        var result = Annotations.EMPTY
        var isSplitModifierList = false

        if (!isNonParenthesizedAnnotationsOnFunctionalTypesEnabled) {
            val targetType = when (modifierListsOwner) {
                is KtNullableType -> modifierListsOwner.innerType
                is KtTypeReference -> modifierListsOwner.typeElement
                else -> null
            }
            val annotationEntries = when (modifierListsOwner) {
                is KtNullableType -> modifierListsOwner.modifierList?.annotationEntries
                is KtTypeReference -> modifierListsOwner.annotationEntries
                else -> null
            }

            // `targetType.stub == null` means that we don't apply this check for files that are built with stubs (that aren't opened in IDE and not in compile time)
            if (targetType is KtFunctionType && targetType.stub == null && annotationEntries != null) {
                checkNonParenthesizedAnnotationsOnFunctionalType(targetType, annotationEntries, trace)
            }
        }

        for (modifierList in modifierLists) {
            if (isSplitModifierList) {
                trace.report(MODIFIER_LIST_NOT_ALLOWED.on(modifierList))
            }

            val annotations = annotationResolver.resolveAnnotationsWithoutArguments(scope, modifierList.annotationEntries, trace)
            result = composeAnnotations(result, annotations)

            isSplitModifierList = true
        }

        return result
    }

    /**
     *  This function is light version of ForceResolveUtil.forceResolveAllContents
     *  We can't use ForceResolveUtil.forceResolveAllContents here because it runs ForceResolveUtil.forceResolveAllContents(getConstructor()),
     *  which is unsafe for some cyclic cases. For Example:
     *  class A: List<A.B> {
     *    class B
     *  }
     *  Here when we resolve class B, we should resolve supertype for A and we shouldn't start resolve for class B,
     *  otherwise it would be a cycle.
     *  Now there is no cycle here because member scope for A is very clever and can get lazy descriptor for class B without resolving it.
     *
     *  todo: find another way after release
     */
    private fun forceResolveTypeContents(type: KotlinType) {
        type.annotations // force read type annotations
        if (type.isFlexible()) {
            forceResolveTypeContents(type.asFlexibleType().lowerBound)
            forceResolveTypeContents(type.asFlexibleType().upperBound)
        } else {
            type.constructor // force read type constructor
            for (projection in type.arguments) {
                if (!projection.isStarProjection) {
                    forceResolveTypeContents(projection.type)
                }
            }
        }
    }

    private fun resolveTypeElement(
        c: TypeResolutionContext,
        annotations: Annotations,
        outerModifierList: KtModifierList?,
        typeElement: KtTypeElement?
    ): PossiblyBareType {
        var result: PossiblyBareType? = null

        val hasSuspendModifier = outerModifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) ?: false
        val suspendModifier by lazy { outerModifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) }
        if (hasSuspendModifier && !typeElement.canHaveFunctionTypeModifiers()) {
            c.trace.report(WRONG_MODIFIER_TARGET.on(suspendModifier!!, KtTokens.SUSPEND_KEYWORD, "non-functional type"))
        } else if (hasSuspendModifier) {
            checkCoroutinesFeature(languageVersionSettings, c.trace, suspendModifier!!)
        }

        typeElement?.accept(object : KtVisitorVoid() {
            override fun visitUserType(type: KtUserType) {
                val qualifierResolutionResult = resolveDescriptorForType(c.scope, type, c.trace, c.isDebuggerContext)
                val classifier = qualifierResolutionResult.classifierDescriptor

                if (classifier == null) {
                    val arguments = resolveTypeProjections(
                        c, ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, typeElement.text).constructor, qualifierResolutionResult.allProjections
                    )
                    val unresolvedType = ErrorUtils.createErrorTypeWithArguments(ErrorTypeKind.UNRESOLVED_TYPE, arguments, type.getDebugText())
                    result = type(unresolvedType)
                    return
                }

                val referenceExpression = type.referenceExpression ?: return

                if (!languageVersionSettings.supportsFeature(LanguageFeature.YieldIsNoMoreReserved)) {
                    checkReservedYield(referenceExpression, c.trace)
                }
                c.trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifier)

                result = resolveTypeForClassifier(c, classifier, qualifierResolutionResult, type, annotations)
            }

            override fun visitNullableType(nullableType: KtNullableType) {
                val innerType = nullableType.innerType

                val baseType = createTypeFromInner(nullableType, nullableType.modifierList, innerType)

                if (!baseType.isBare && baseType.actualType is DefinitelyNotNullType) {
                    c.trace.report(NULLABLE_ON_DEFINITELY_NOT_NULLABLE.on(nullableType))
                }

                if (baseType.isNullable || innerType is KtNullableType || innerType is KtDynamicType) {
                    c.trace.report(REDUNDANT_NULLABLE.on(nullableType))
                }
                result = baseType.makeNullable()
            }

            private fun createTypeFromInner(
                typeElement: KtTypeElement,
                innerModifierList: KtModifierList?,
                innerType: KtTypeElement?
            ): PossiblyBareType {
                if (innerModifierList != null && outerModifierList != null) {
                    c.trace.report(MODIFIER_LIST_NOT_ALLOWED.on(innerModifierList))
                }

                val innerAnnotations = composeAnnotations(
                    annotations,
                    resolveTypeAnnotations(c.trace, c.scope, typeElement as KtElementImplStub<*>)
                )

                return resolveTypeElement(c, innerAnnotations, outerModifierList ?: innerModifierList, innerType)
            }

            override fun visitIntersectionType(intersectionType: KtIntersectionType) {
                val leftType = resolvePossiblyBareType(c, intersectionType.getLeftTypeRef() ?: return).let {
                    when {
                        it.isBare -> error("There should not be bare types for intersections")
                        else -> it.actualType
                    }
                }

                // Just in case of early return
                result = type(leftType)

                val rightType = resolvePossiblyBareType(c, intersectionType.getRightTypeRef() ?: return).let {
                    when {
                        it.isBare -> error("There should not be bare types for intersections")
                        else -> it.actualType
                    }
                }

                if (!languageVersionSettings.supportsFeature(LanguageFeature.DefinitelyNonNullableTypes)) {
                    c.trace.report(
                        UNSUPPORTED_FEATURE.on(
                            intersectionType,
                            LanguageFeature.DefinitelyNonNullableTypes to languageVersionSettings
                        )
                    )
                    return
                }

                if (!leftType.isTypeParameter() || leftType.isMarkedNullable || !leftType.isNullableOrUninitializedTypeParameter()) {
                    c.trace.report(INCORRECT_LEFT_COMPONENT_OF_INTERSECTION.on(intersectionType.getLeftTypeRef()!!))
                    return
                }

                if (!rightType.isAny()) {
                    c.trace.report(INCORRECT_RIGHT_COMPONENT_OF_INTERSECTION.on(intersectionType.getRightTypeRef()!!))
                    return
                }

                val definitelyNotNullType =
                    DefinitelyNotNullType.makeDefinitelyNotNull(leftType.unwrap())
                        ?: error(
                            "Definitely not-nullable type is not created for type parameter with nullable upper bound ${
                                TypeUtils.getTypeParameterDescriptorOrNull(
                                    leftType
                                )!!
                            }"
                        )

                result = type(definitelyNotNullType)
            }

            private fun KotlinType.isNullableOrUninitializedTypeParameter(): Boolean {
                if ((constructor.declarationDescriptor as? TypeParameterDescriptorImpl)?.isInitialized == false) {
                    return true
                }

                return isNullable()
            }

            override fun visitFunctionType(type: KtFunctionType) {
                val receiverTypeRef = type.receiverTypeReference
                val receiverType = if (receiverTypeRef?.typeElement == null) null else resolveType(c.noBareTypes(), receiverTypeRef)

                val contextReceiverList = type.contextReceiverList
                val contextReceiversTypes = if (contextReceiverList != null) {
                    checkContextReceiversAreEnabled(contextReceiverList)
                    contextReceiverList.typeReferences().map { typeRef ->
                        resolveType(c.noBareTypes(), typeRef)
                    }
                } else emptyList()

                val parameterDescriptors = resolveParametersOfFunctionType(type.parameters)
                checkParametersOfFunctionType(parameterDescriptors)

                val returnTypeRef = type.returnTypeReference
                val returnType = if (returnTypeRef != null) resolveType(c.noBareTypes(), returnTypeRef)
                else moduleDescriptor.builtIns.unitType

                val parameterList = type.parameterList
                if (parameterList?.stub == null) {
                    TrailingCommaChecker.check(parameterList?.trailingComma, c.trace, languageVersionSettings)
                }

                result = type(
                    createFunctionType(
                        moduleDescriptor.builtIns, annotations, receiverType, contextReceiversTypes,
                        parameterDescriptors.map { it.type },
                        parameterDescriptors.map { it.name },
                        returnType,
                        suspendFunction = hasSuspendModifier
                    )
                )
            }

            private fun checkParametersOfFunctionType(parameterDescriptors: List<VariableDescriptor>) {
                val parametersByName = parameterDescriptors.filter { !it.name.isSpecial }.groupBy { it.name }
                for (parametersGroup in parametersByName.values) {
                    if (parametersGroup.size < 2) continue
                    for (parameter in parametersGroup) {
                        val ktParameter = parameter.source.getPsi()?.safeAs<KtParameter>() ?: continue
                        c.trace.report(DUPLICATE_PARAMETER_NAME_IN_FUNCTION_TYPE.on(ktParameter))
                    }
                }
            }

            private fun resolveParametersOfFunctionType(parameters: List<KtParameter>): List<VariableDescriptor> {
                class ParameterOfFunctionTypeDescriptor(
                    containingDeclaration: DeclarationDescriptor,
                    annotations: Annotations,
                    name: Name,
                    type: KotlinType,
                    source: SourceElement
                ) : VariableDescriptorImpl(containingDeclaration, annotations, name, type, source) {
                    override fun getVisibility() = DescriptorVisibilities.LOCAL

                    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? {
                        throw UnsupportedOperationException("Should not be called for descriptor of type ${this::class.java}")
                    }

                    override fun isVar() = false

                    override fun isLateInit() = false

                    override fun getCompileTimeInitializer() = null

                    override fun cleanCompileTimeInitializerCache() {}

                    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
                        return visitor.visitVariableDescriptor(this, data)
                    }
                }

                parameters.forEach {
                    identifierChecker.checkDeclaration(it, c.trace)
                    checkParameterInFunctionType(it)
                }
                return parameters.map { parameter ->
                    val parameterType = resolveType(c.noBareTypes(), parameter.typeReference!!)
                    val descriptor = ParameterOfFunctionTypeDescriptor(
                        c.scope.ownerDescriptor,
                        annotationResolver.resolveAnnotationsWithoutArguments(c.scope, parameter.modifierList, c.trace),
                        parameter.nameAsSafeName,
                        parameterType,
                        parameter.toSourceElement()
                    )
                    c.trace.record(BindingContext.VALUE_PARAMETER, parameter, descriptor)
                    descriptor
                }
            }

            override fun visitContextReceiverList(contextReceiverList: KtContextReceiverList) {
                checkContextReceiversAreEnabled(contextReceiverList)
            }

            override fun visitDynamicType(type: KtDynamicType) {
                result = type(dynamicCallableDescriptors.dynamicType.replaceAnnotations(annotations))
                if (!dynamicTypesSettings.dynamicTypesAllowed) {
                    c.trace.report(UNSUPPORTED.on(type, "Dynamic types are not supported in this context"))
                }
            }

            override fun visitSelfType(type: KtSelfType) {
                c.trace.report(UNSUPPORTED.on(type, "Self-types are not supported"))
            }

            override fun visitKtElement(element: KtElement) {
                c.trace.report(UNSUPPORTED.on(element, "Self-types are not supported yet"))
            }

            private fun checkParameterInFunctionType(param: KtParameter) {
                if (param.hasDefaultValue()) {
                    c.trace.report(Errors.UNSUPPORTED.on(param.defaultValue!!, "default value of parameter in function type"))
                }

                if (param.name != null) {
                    for (annotationEntry in param.annotationEntries) {
                        c.trace.report(Errors.UNSUPPORTED.on(annotationEntry, "annotation on parameter in function type"))
                    }
                }

                val modifierList = param.modifierList
                if (modifierList != null) {
                    KtTokens.MODIFIER_KEYWORDS_ARRAY
                        .mapNotNull { modifierList.getModifier(it) }
                        .forEach {
                            c.trace.report(Errors.UNSUPPORTED.on(it, "modifier on parameter in function type"))
                        }
                }

                param.valOrVarKeyword?.let {
                    c.trace.report(Errors.UNSUPPORTED.on(it, "val or var on parameter in function type"))
                }
            }

            private fun checkContextReceiversAreEnabled(contextReceiverList: KtContextReceiverList) {
                if (!languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
                    c.trace.report(
                        UNSUPPORTED_FEATURE.on(
                            contextReceiverList,
                            LanguageFeature.ContextReceivers to languageVersionSettings
                        )
                    )
                }
            }
        })

        return result ?: type(ErrorUtils.createErrorType(ErrorTypeKind.NO_TYPE_SPECIFIED, typeElement?.getDebugText() ?: "unknown element"))
    }

    private fun KtTypeElement?.canHaveFunctionTypeModifiers(): Boolean =
        this is KtFunctionType

    private fun resolveTypeForTypeParameter(
        c: TypeResolutionContext, annotations: Annotations,
        typeParameter: TypeParameterDescriptor,
        referenceExpression: KtSimpleNameExpression,
        typeArgumentList: KtTypeArgumentList?
    ): KotlinType {
        val scopeForTypeParameter = getScopeForTypeParameter(c, typeParameter)

        if (typeArgumentList != null) {
            resolveTypeProjections(c, ErrorUtils.createErrorType(ErrorTypeKind.ERROR_TYPE_PARAMETER).constructor, typeArgumentList.arguments)
            c.trace.report(TYPE_ARGUMENTS_NOT_ALLOWED.on(typeArgumentList, "for type parameters"))
        }

        val containing = typeParameter.containingDeclaration
        if (containing is ClassDescriptor) {
            DescriptorResolver.checkHasOuterClassInstance(c.scope, c.trace, referenceExpression, containing)
        }

        return if (scopeForTypeParameter is ErrorScope && scopeForTypeParameter !is ThrowingScope)
            ErrorUtils.createErrorType(ErrorTypeKind.ERROR_TYPE_PARAMETER)
        else
            KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                typeAttributeTranslators.toAttributes(annotations, typeParameter.typeConstructor, containing),
                typeParameter.typeConstructor,
                listOf(),
                false,
                scopeForTypeParameter
            )
    }

    private fun getScopeForTypeParameter(c: TypeResolutionContext, typeParameterDescriptor: TypeParameterDescriptor): MemberScope {
        return when {
            c.checkBounds -> TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor).memberScope
            else -> LazyScopeAdapter {
                TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor).memberScope
            }
        }
    }

    fun resolveTypeForClassifier(
        c: TypeResolutionContext,
        descriptor: ClassifierDescriptor,
        qualifierResolutionResult: QualifiedExpressionResolver.TypeQualifierResolutionResult,
        element: KtElement,
        annotations: Annotations
    ): PossiblyBareType {
        val qualifierParts = qualifierResolutionResult.qualifierParts

        if (element is KtUserType && element.stub == null) {
            TrailingCommaChecker.check(element.typeArgumentList?.trailingComma, c.trace, languageVersionSettings)
        }

        return when (descriptor) {
            is TypeParameterDescriptor -> {
                assert(qualifierParts.size == 1) {
                    "Type parameter can be resolved only by it's short name, but '${element.text}' is contradiction " +
                            "with ${qualifierParts.size} qualifier parts"
                }

                val qualifierPart = qualifierParts.single()
                type(resolveTypeForTypeParameter(c, annotations, descriptor, qualifierPart.expression, qualifierPart.typeArguments))
            }
            is ClassDescriptor -> resolveTypeForClass(c, annotations, descriptor, element, qualifierResolutionResult)
            is TypeAliasDescriptor -> resolveTypeForTypeAlias(c, annotations, descriptor, element, qualifierResolutionResult)
            else -> error("Unexpected classifier type: ${descriptor::class.java}")
        }
    }

    private fun resolveTypeForClass(
        c: TypeResolutionContext, annotations: Annotations,
        classDescriptor: ClassDescriptor, element: KtElement,
        qualifierResolutionResult: QualifiedExpressionResolver.TypeQualifierResolutionResult
    ): PossiblyBareType {
        val typeConstructor = classDescriptor.typeConstructor

        val projectionFromAllQualifierParts = qualifierResolutionResult.allProjections
        val parameters = typeConstructor.parameters
        if (c.allowBareTypes && projectionFromAllQualifierParts.isEmpty() && isPossibleToSpecifyTypeArgumentsFor(classDescriptor)) {
            // See docs for PossiblyBareType
            return PossiblyBareType.bare(typeConstructor, false)
        }

        if (ErrorUtils.isError(classDescriptor)) {
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }

        val (collectedArgumentAsTypeProjections, argumentsForOuterClass) =
            collectArgumentsForClassifierTypeConstructor(c, classDescriptor, qualifierResolutionResult.qualifierParts)
                ?: return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)

        assert(collectedArgumentAsTypeProjections.size <= parameters.size) {
            "Collected arguments count should be not greater then parameters count," +
                    " but ${collectedArgumentAsTypeProjections.size} instead of ${parameters.size} found in ${element.text}"
        }

        val argumentsFromUserType = resolveTypeProjections(c, typeConstructor, collectedArgumentAsTypeProjections)
        val arguments = buildFinalArgumentList(argumentsFromUserType, argumentsForOuterClass, parameters)

        assert(arguments.size == parameters.size) {
            "Collected arguments count should be equal to parameters count," +
                    " but ${collectedArgumentAsTypeProjections.size} instead of ${parameters.size} found in ${element.text}"
        }

        val resultingType =
            KotlinTypeFactory.simpleNotNullType(
                typeAttributeTranslators.toAttributes(annotations, classDescriptor.typeConstructor, c.scope.ownerDescriptor),
                classDescriptor,
                arguments
            )

        // We create flexible types by convention here
        // This is not intended to be used in normal users' environments, only for tests and debugger etc
        typeTransformerForTests.transformType(resultingType)?.let { return type(it) }

        if (shouldCheckBounds(c, resultingType)) {
            val substitutor = TypeSubstitutor.create(resultingType)
            for (i in parameters.indices) {
                val parameter = parameters[i]
                val argument = arguments[i].type

                val typeReference = collectedArgumentAsTypeProjections.getOrNull(i)?.typeReference

                if (typeReference != null) {
                    upperBoundChecker.checkBounds(typeReference, argument, parameter, substitutor, c.trace)
                }
            }
        }

        if (resultingType.isArrayOfNothing()) {
            c.trace.report(UNSUPPORTED.on(element, "Array<Nothing> is illegal"))
        }

        return type(resultingType)
    }

    private fun buildFinalArgumentList(
        argumentsFromUserType: List<TypeProjection>,
        argumentsForOuterClass: List<TypeProjection>?,
        parameters: List<TypeParameterDescriptor>
    ): List<TypeProjection> {
        return argumentsFromUserType +
                (argumentsForOuterClass ?: appendDefaultArgumentsForLocalClassifier(argumentsFromUserType.size, parameters))
    }

    private fun shouldCheckBounds(c: TypeResolutionContext, inType: KotlinType): Boolean {
        if (!c.checkBounds) return false
        if (inType.containsTypeAliasParameters()) return false
        if (c.abbreviated && inType.containsTypeAliases()) return false

        return true
    }

    private fun resolveTypeForTypeAlias(
        c: TypeResolutionContext,
        annotations: Annotations,
        descriptor: TypeAliasDescriptor,
        type: KtElement,
        qualifierResolutionResult: QualifiedExpressionResolver.TypeQualifierResolutionResult
    ): PossiblyBareType {
        val typeConstructor = descriptor.typeConstructor
        val projectionFromAllQualifierParts = qualifierResolutionResult.allProjections

        if (ErrorUtils.isError(descriptor)) {
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }
        if (!languageVersionSettings.supportsFeature(LanguageFeature.TypeAliases)) {
            c.trace.report(UNSUPPORTED_FEATURE.on(type, LanguageFeature.TypeAliases to languageVersionSettings))
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }

        val parameters = typeConstructor.parameters

        if (c.allowBareTypes && projectionFromAllQualifierParts.isEmpty() && isPossibleToSpecifyTypeArgumentsFor(descriptor)) {
            val classDescriptor = descriptor.classDescriptor
            if (classDescriptor != null && canBeUsedAsBareType(descriptor)) {
                return bare(descriptor.classDescriptor!!.typeConstructor, TypeUtils.isNullableType(descriptor.expandedType))
            }
        }

        val typeAliasQualifierPart =
            qualifierResolutionResult.qualifierParts.lastOrNull()
                ?: return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)

        val (argumentElementsFromUserType, argumentsForOuterClass) =
            collectArgumentsForClassifierTypeConstructor(c, descriptor, qualifierResolutionResult.qualifierParts)
                ?: return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)

        val argumentsFromUserType = resolveTypeProjections(c, typeConstructor, argumentElementsFromUserType)

        val arguments = buildFinalArgumentList(argumentsFromUserType, argumentsForOuterClass, parameters)

        val reportStrategy = TracingTypeAliasExpansionReportStrategy(
            c.trace,
            type, typeAliasQualifierPart.typeArguments ?: typeAliasQualifierPart.expression,
            descriptor, descriptor.declaredTypeParameters,
            argumentElementsFromUserType, // TODO arguments from inner scope
            upperBoundChecker
        )

        if (parameters.size != arguments.size) {
            reportStrategy.wrongNumberOfTypeArguments(descriptor, parameters.size)
            return createErrorTypeForTypeConstructor(c, projectionFromAllQualifierParts, typeConstructor)
        }

        val attributes = typeAttributeTranslators.toAttributes(annotations, descriptor.typeConstructor, c.scope.ownerDescriptor)

        return if (c.abbreviated) {
            val abbreviatedType = KotlinTypeFactory.simpleType(
                attributes,
                descriptor.typeConstructor,
                arguments,
                false
            )
            type(abbreviatedType)
        } else {
            val typeAliasExpansion = TypeAliasExpansion.create(null, descriptor, arguments)
            val expandedType = TypeAliasExpander(reportStrategy, c.checkBounds).expand(typeAliasExpansion, attributes)
            type(expandedType)
        }
    }

    /**
     * Type alias can be used as bare type (after is/as, e.g., 'x is List')
     * iff all type arguments of the corresponding expanded type are either star projections
     * or type parameters of the given type alias in invariant projection,
     * and each of the type parameters is mentioned no more than once.
     *
     * E.g.:
     * ```
     * typealias HashMap<K, V> = java.util.HashMap<K, V>    // can be used as bare type
     * typealias MyList<T, X> = List<X>                     // can be used as bare type
     * typealias StarMap<T> = Map<T, *>                     // can be used as bare type
     * typealias MyMap<T> = Map<T, T>                       // CAN NOT be used as bare type: type parameter 'T' is used twice
     * typealias StringMap<T> = Map<String, T>              // CAN NOT be used as bare type: type argument 'String' is not a type parameter
     * ```
     */
    private fun canBeUsedAsBareType(descriptor: TypeAliasDescriptor): Boolean {
        val expandedType = descriptor.expandedType
        if (expandedType.isError) return false

        val classDescriptor = descriptor.classDescriptor ?: return false
        if (!isPossibleToSpecifyTypeArgumentsFor(classDescriptor)) return false

        val usedTypeParameters = linkedSetOf<TypeParameterDescriptor>()
        for (argument in expandedType.arguments) {
            if (argument.isStarProjection) continue

            if (argument.projectionKind != INVARIANT) return false

            val argumentTypeDescriptor = argument.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
            if (argumentTypeDescriptor.containingDeclaration != descriptor) return false
            if (usedTypeParameters.contains(argumentTypeDescriptor)) return false

            usedTypeParameters.add(argumentTypeDescriptor)
        }

        return true
    }

    private class TracingTypeAliasExpansionReportStrategy(
        val trace: BindingTrace,
        val type: KtElement?,
        val typeArgumentsOrTypeName: KtElement?,
        val typeAliasDescriptor: TypeAliasDescriptor,
        typeParameters: List<TypeParameterDescriptor>,
        typeArguments: List<KtTypeProjection>,
        val upperBoundChecker: UpperBoundChecker
    ) : TypeAliasExpansionReportStrategy {

        private val mappedArguments = typeParameters.zip(typeArguments).toMap()

        override fun wrongNumberOfTypeArguments(typeAlias: TypeAliasDescriptor, numberOfParameters: Int) {
            if (typeArgumentsOrTypeName != null) {
                trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(typeArgumentsOrTypeName, numberOfParameters, typeAliasDescriptor))
            }
        }

        override fun conflictingProjection(
            typeAlias: TypeAliasDescriptor,
            typeParameter: TypeParameterDescriptor?,
            substitutedArgument: KotlinType
        ) {
            val argumentElement = typeParameter?.let { mappedArguments[it] }
            if (argumentElement != null) {
                trace.report(CONFLICTING_PROJECTION.on(argumentElement, typeParameter))
            } else if (type != null) {
                trace.report(CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION.on(type, typeAliasDescriptor.underlyingType))
            }
        }

        override fun recursiveTypeAlias(typeAlias: TypeAliasDescriptor) {
            if (type != null) {
                trace.report(RECURSIVE_TYPEALIAS_EXPANSION.on(type, typeAlias))
            }
        }

        override fun boundsViolationInSubstitution(
            substitutor: TypeSubstitutor,
            unsubstitutedArgument: KotlinType,
            argument: KotlinType,
            typeParameter: TypeParameterDescriptor
        ) {
            val descriptorForUnsubstitutedArgument = unsubstitutedArgument.constructor.declarationDescriptor
            val argumentElement = mappedArguments[descriptorForUnsubstitutedArgument]
            val argumentTypeReferenceElement = argumentElement?.typeReference
            upperBoundChecker.checkBounds(argumentTypeReferenceElement, argument, typeParameter, substitutor, trace, type)
        }

        override fun repeatedAnnotation(annotation: AnnotationDescriptor) {
            val annotationEntry = (annotation.source as? KotlinSourceElement)?.psi as? KtAnnotationEntry ?: return
            trace.report(REPEATED_ANNOTATION.on(annotationEntry))
        }
    }

    private fun createErrorTypeForTypeConstructor(
        c: TypeResolutionContext,
        arguments: List<KtTypeProjection>,
        typeConstructor: TypeConstructor
    ): PossiblyBareType =
        type(
            ErrorUtils.createErrorTypeWithArguments(
                ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR,
                resolveTypeProjectionsWithErrorConstructor(c, arguments),
                typeConstructor.declarationDescriptor?.name?.asString() ?: typeConstructor.toString()
            )
        )

    // Returns true in case when at least one argument for this class could be specified
    // It could be always equal to 'typeConstructor.parameters.isNotEmpty()' unless local classes could captured type parameters
    // from enclosing functions. In such cases you can not specify any argument:
    // fun <E> foo(x: Any?) {
    //    class C
    //    if (x is C) { // 'C' should not be treated as bare type here
    //       ...
    //    }
    // }
    //
    // It's needed to determine whether this particular type could be bare
    private fun isPossibleToSpecifyTypeArgumentsFor(classifierDescriptor: ClassifierDescriptorWithTypeParameters): Boolean {
        // First parameter relates to the innermost declaration
        // If it's declared in function there
        val firstTypeParameter = classifierDescriptor.typeConstructor.parameters.firstOrNull() ?: return false
        return firstTypeParameter.original.containingDeclaration is ClassifierDescriptorWithTypeParameters
    }

    /**
     * @return yet unresolved KtTypeProjection arguments and already resolved ones relevant to an outer class
     * @return null if error was reported
     *
     * If second component is null then rest of the arguments should be appended using default types of relevant parameters
     */
    private fun collectArgumentsForClassifierTypeConstructor(
        c: TypeResolutionContext,
        classifierDescriptor: ClassifierDescriptorWithTypeParameters,
        qualifierParts: List<QualifiedExpressionResolver.ExpressionQualifierPart>
    ): Pair<List<KtTypeProjection>, List<TypeProjection>?>? {
        val classifierDescriptorChain = classifierDescriptor.classifierDescriptorsFromInnerToOuter()
        val reversedQualifierParts = qualifierParts.asReversed()

        var wasStatic = false
        val result = SmartList<KtTypeProjection>()

        val classifierChainLastIndex = min(classifierDescriptorChain.size, reversedQualifierParts.size) - 1

        for (index in 0..classifierChainLastIndex) {
            val qualifierPart = reversedQualifierParts[index]
            val currentArguments = qualifierPart.typeArguments?.arguments.orEmpty()
            val declaredTypeParameters = classifierDescriptorChain[index].declaredTypeParameters
            val currentParameters = if (wasStatic) emptyList() else declaredTypeParameters

            if (wasStatic && currentArguments.isNotEmpty() && declaredTypeParameters.isNotEmpty()) {
                c.trace.report(TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED.on(qualifierPart.typeArguments!!))
                return null
            }

            if (currentArguments.size != currentParameters.size) {
                c.trace.report(
                    WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                        qualifierPart.typeArguments ?: qualifierPart.expression,
                        currentParameters.size, classifierDescriptorChain[index]
                    )
                )
                return null
            }

            result.addAll(currentArguments)

            wasStatic = wasStatic || !classifierDescriptorChain[index].isInner
        }

        val nonClassQualifierParts =
            reversedQualifierParts.subList(
                min(classifierChainLastIndex + 1, reversedQualifierParts.size),
                reversedQualifierParts.size
            )

        for ((_, _, typeArguments) in nonClassQualifierParts) {
            if (typeArguments != null) {
                c.trace.report(TYPE_ARGUMENTS_NOT_ALLOWED.on(typeArguments, "here"))
                return null
            }
        }

        val parameters = classifierDescriptor.typeConstructor.parameters
        if (result.size < parameters.size) {
            val nextParameterOwner =
                parameters[result.size].original.containingDeclaration as? ClassDescriptor
                // If next parameter is captured from the enclosing function, default arguments must be used
                // (see appendDefaultArgumentsForLocalClassifier)
                    ?: return Pair(result, null)

            val restArguments = c.scope.findImplicitOuterClassArguments(nextParameterOwner)
            val restParameters = parameters.subList(result.size, parameters.size)

            val typeArgumentsCanBeSpecifiedCount =
                classifierDescriptor.classifierDescriptorsFromInnerToOuter().sumOf { it.declaredTypeParameters.size }

            if (restArguments == null && typeArgumentsCanBeSpecifiedCount > result.size) {
                c.trace.report(
                    OUTER_CLASS_ARGUMENTS_REQUIRED.on(qualifierParts.first().expression, nextParameterOwner)
                )
                return null
            } else if (restArguments == null) {
                assert(typeArgumentsCanBeSpecifiedCount == result.size) {
                    "Number of type arguments that can be specified ($typeArgumentsCanBeSpecifiedCount) " +
                            "should be equal to actual arguments number ${result.size}, (classifier: $classifierDescriptor)"
                }
                return Pair(result, null)
            } else {
                assert(restParameters.size == restArguments.size) {
                    "Number of type of restParameters should be equal to ${restParameters.size}, " +
                            "but ${restArguments.size} were found for $classifierDescriptor/$nextParameterOwner"
                }

                return Pair(result, restArguments)
            }
        }

        return Pair(result, null)
    }

    private fun ClassifierDescriptor?.classifierDescriptorsFromInnerToOuter(): List<ClassifierDescriptorWithTypeParameters> =
        generateSequence(
            { this as? ClassifierDescriptorWithTypeParameters },
            { it.containingDeclaration as? ClassifierDescriptorWithTypeParameters }
        ).toList()

    private fun resolveTypeProjectionsWithErrorConstructor(
        c: TypeResolutionContext,
        argumentElements: List<KtTypeProjection>,
        message: String = "Error type for resolving type projections"
    ) = resolveTypeProjections(c, ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.TYPE_FOR_ERROR_TYPE_CONSTRUCTOR, message), argumentElements)

    /**
     * For cases like:
     * fun <E> foo() {
     *  class Local<F>
     *  val x: Local<Int> <-- resolve this type
     * }
     *
     * type constructor for `Local` captures type parameter E from containing outer function
     */
    private fun appendDefaultArgumentsForLocalClassifier(
        fromIndex: Int,
        constructorParameters: List<TypeParameterDescriptor>
    ) = constructorParameters.subList(fromIndex, constructorParameters.size).map {
        TypeProjectionImpl(it.original.defaultType)
    }

    fun resolveTypeProjections(
        c: TypeResolutionContext,
        constructor: TypeConstructor,
        argumentElements: List<KtTypeProjection>
    ): List<TypeProjection> {
        return argumentElements.mapIndexed { i, argumentElement ->
            val projectionKind = argumentElement.projectionKind
            ModifierCheckerCore.check(argumentElement, c.trace, null, languageVersionSettings)
            if (projectionKind == KtProjectionKind.STAR) {
                val parameters = constructor.parameters
                if (parameters.size > i) {
                    val parameterDescriptor = parameters[i]
                    TypeUtils.makeStarProjection(parameterDescriptor)
                } else {
                    TypeProjectionImpl(OUT_VARIANCE, ErrorUtils.createErrorType(ErrorTypeKind.ERROR_TYPE_PROJECTION))
                }
            } else {
                val type = resolveType(c.noBareTypes(), argumentElement.typeReference!!)
                val kind = resolveProjectionKind(projectionKind)
                if (constructor.parameters.size > i) {
                    val parameterDescriptor = constructor.parameters[i]
                    if (kind != INVARIANT && parameterDescriptor.variance != INVARIANT) {
                        if (kind == parameterDescriptor.variance) {
                            c.trace.report(REDUNDANT_PROJECTION.on(argumentElement, constructor.declarationDescriptor!!))
                        } else {
                            c.trace.report(CONFLICTING_PROJECTION.on(argumentElement, constructor.declarationDescriptor!!))
                        }
                    }
                }
                TypeProjectionImpl(kind, type)
            }

        }
    }

    private fun LexicalScope.findImplicitOuterClassArguments(
        outerClass: ClassDescriptor
    ): List<TypeProjection>? {
        val enclosingClass = findFirstFromMeAndParent { scope ->
            if (scope is LexicalScope && scope.kind == LexicalScopeKind.CLASS_MEMBER_SCOPE)
                scope.ownerDescriptor as ClassDescriptor
            else
                null
        } ?: return null

        return findImplicitOuterClassArguments(enclosingClass, outerClass)
    }

    fun resolveClass(
        scope: LexicalScope, userType: KtUserType, trace: BindingTrace, isDebuggerContext: Boolean
    ): ClassifierDescriptor? = resolveDescriptorForType(scope, userType, trace, isDebuggerContext).classifierDescriptor

    fun resolveDescriptorForType(
        scope: LexicalScope, userType: KtUserType, trace: BindingTrace, isDebuggerContext: Boolean
    ): QualifiedExpressionResolver.TypeQualifierResolutionResult {
        if (userType.qualifier != null) { // we must resolve all type references in arguments of qualifier type
            for (typeArgument in userType.qualifier!!.typeArguments) {
                typeArgument.typeReference?.let {
                    // in qualified expression, type argument can have bounds only in incorrect code
                    forceResolveTypeContents(resolveType(scope, it, trace, false))
                }
            }
        }

        return qualifiedExpressionResolver.resolveDescriptorForType(userType, scope, trace, isDebuggerContext).apply {
            if (classifierDescriptor != null) {
                PlatformClassesMappedToKotlinChecker.reportPlatformClassMappedToKotlin(
                    platformToKotlinClassMapper, trace, userType, classifierDescriptor
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun resolveProjectionKind(projectionKind: KtProjectionKind): Variance {
            return when (projectionKind) {
                KtProjectionKind.IN -> IN_VARIANCE
                KtProjectionKind.OUT -> OUT_VARIANCE
                KtProjectionKind.NONE -> INVARIANT
                else -> // NOTE: Star projections must be handled before this method is called
                    throw IllegalStateException("Illegal projection kind:" + projectionKind)
            }
        }
    }
}
