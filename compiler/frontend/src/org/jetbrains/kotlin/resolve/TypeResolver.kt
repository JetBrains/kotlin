/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.context.TypeLazinessToken
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.VariableDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.debugTypeInfo
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.resolve.PossiblyBareType.type
import org.jetbrains.kotlin.resolve.bindingContextUtil.recordScope
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.typeUtil.isArrayOfNothing

class TypeResolver(
        private val annotationResolver: AnnotationResolver,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val moduleDescriptor: ModuleDescriptor,
        private val flexibleTypeCapabilitiesProvider: FlexibleTypeCapabilitiesProvider,
        private val storageManager: StorageManager,
        private val lazinessToken: TypeLazinessToken,
        private val dynamicTypesSettings: DynamicTypesSettings,
        private val dynamicCallableDescriptors: DynamicCallableDescriptors,
        private val identifierChecker: IdentifierChecker
) {

    open class FlexibleTypeCapabilitiesProvider {
        open fun getCapabilities(): FlexibleTypeCapabilities {
            return FlexibleTypeCapabilities.NONE
        }
    }

    fun resolveType(scope: LexicalScope, typeReference: KtTypeReference, trace: BindingTrace, checkBounds: Boolean): KotlinType {
        // bare types are not allowed
        return resolveType(TypeResolutionContext(scope, trace, checkBounds, false), typeReference)
    }

    private fun resolveType(c: TypeResolutionContext, typeReference: KtTypeReference): KotlinType {
        assert(!c.allowBareTypes) { "Use resolvePossiblyBareType() when bare types are allowed" }
        return resolvePossiblyBareType(c, typeReference).getActualType()
    }

    fun resolvePossiblyBareType(c: TypeResolutionContext, typeReference: KtTypeReference): PossiblyBareType {
        val cachedType = c.trace.getBindingContext().get(BindingContext.TYPE, typeReference)
        if (cachedType != null) return type(cachedType)

        val debugType = typeReference.debugTypeInfo
        if (debugType != null) {
            c.trace.record(BindingContext.TYPE, typeReference, debugType)
            return type(debugType)
        }

        if (!c.allowBareTypes && !c.forceResolveLazyTypes && lazinessToken.isLazy()) {
            // Bare types can be allowed only inside expressions; lazy type resolution is only relevant for declarations
            class LazyKotlinType : DelegatingType(), LazyEntity {
                private val _delegate = storageManager.createLazyValue { doResolvePossiblyBareType(c, typeReference).getActualType() }
                override fun getDelegate() = _delegate()

                override fun forceResolveAllContents() {
                    ForceResolveUtil.forceResolveAllContents(getConstructor())
                    getArguments().forEach { ForceResolveUtil.forceResolveAllContents(it.getType()) }
                }
            }

            val lazyKotlinType = LazyKotlinType()
            c.trace.record(BindingContext.TYPE, typeReference, lazyKotlinType)
            return type(lazyKotlinType);
        }

        val type = doResolvePossiblyBareType(c, typeReference)
        if (!type.isBare()) {
            c.trace.record(BindingContext.TYPE, typeReference, type.getActualType())
        }
        return type
    }

    private fun doResolvePossiblyBareType(c: TypeResolutionContext, typeReference: KtTypeReference): PossiblyBareType {
        val annotations = annotationResolver.resolveAnnotationsWithoutArguments(c.scope, typeReference.getAnnotationEntries(), c.trace)

        val typeElement = typeReference.typeElement

        val type = resolveTypeElement(c, annotations, typeElement)
        c.trace.recordScope(c.scope, typeReference)

        if (!type.isBare) {
            for (argument in type.actualType.arguments) {
                forceResolveTypeContents(argument.type)
            }
        }

        return type
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
            forceResolveTypeContents(type.flexibility().lowerBound)
            forceResolveTypeContents(type.flexibility().upperBound)
        }
        else {
            type.constructor // force read type constructor
            for (projection in type.arguments) {
                if (!projection.isStarProjection) {
                    forceResolveTypeContents(projection.type)
                }
            }
        }
    }

    private fun resolveTypeElement(c: TypeResolutionContext, annotations: Annotations, typeElement: KtTypeElement?): PossiblyBareType {
        var result: PossiblyBareType? = null
        typeElement?.accept(object : KtVisitorVoid() {
            override fun visitUserType(type: KtUserType) {
                val qualifierResolutionResults = resolveDescriptorForType(c.scope, type, c.trace)
                val (qualifierParts, classifierDescriptor) = qualifierResolutionResults

                if (classifierDescriptor == null) {
                    val arguments = resolveTypeProjections(
                            c, ErrorUtils.createErrorType("No type").constructor, qualifierResolutionResults.allProjections)
                    result = type(ErrorUtils.createErrorTypeWithArguments(type.getDebugText(), arguments))
                    return
                }

                val referenceExpression = type.getReferenceExpression()
                val referencedName = type.getReferencedName()
                if (referenceExpression == null || referencedName == null) return

                c.trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor)

                result = when (classifierDescriptor) {
                    is TypeParameterDescriptor -> {
                        assert(qualifierParts.size == 1) {
                            "Type parameter can be resolved only by it's short name, but '${type.text}' is contradiction " +
                            "with ${qualifierParts.size} qualifier parts"
                        }

                        type(resolveTypeForTypeParameter(c, annotations, classifierDescriptor, referenceExpression, type))
                    }
                    is ClassDescriptor -> resolveTypeForClass(c, annotations, classifierDescriptor, type, qualifierResolutionResults)
                    else -> error("Unexpected classifier type: ${classifierDescriptor.javaClass}")
                }
            }

            override fun visitNullableType(nullableType: KtNullableType) {
                val innerType = nullableType.getInnerType()
                val baseType = resolveTypeElement(c, annotations, innerType)
                if (baseType.isNullable || innerType is KtNullableType || innerType is KtDynamicType) {
                    c.trace.report(REDUNDANT_NULLABLE.on(nullableType))
                }
                result = baseType.makeNullable()
            }

            override fun visitFunctionType(type: KtFunctionType) {
                val receiverTypeRef = type.receiverTypeReference
                val receiverType = if (receiverTypeRef == null) null else resolveType(c.noBareTypes(), receiverTypeRef)

                val parameterDescriptors = resolveParametersOfFunctionType(type.parameters)

                val returnTypeRef = type.returnTypeReference
                val returnType = if (returnTypeRef != null) resolveType(c.noBareTypes(), returnTypeRef)
                                 else moduleDescriptor.builtIns.unitType

                result = type(
                        moduleDescriptor.builtIns.getFunctionType(
                                annotations, receiverType, parameterDescriptors.map { it.type }, returnType
                        )
                )
            }

            private fun resolveParametersOfFunctionType(parameters: List<KtParameter>): List<VariableDescriptor> {
                class ParameterOfFunctionTypeDescriptor(
                        containingDeclaration: DeclarationDescriptor,
                        annotations: Annotations,
                        name: Name,
                        type: KotlinType,
                        source: SourceElement
                ) : VariableDescriptorImpl(containingDeclaration, annotations, name, type, source) {
                    override fun getVisibility() = Visibilities.LOCAL

                    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? {
                        throw UnsupportedOperationException("Should not be called for descriptor of type $javaClass")
                    }

                    override fun isVar() = false

                    override fun getCompileTimeInitializer() = null

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

            override fun visitDynamicType(type: KtDynamicType) {
                result = type(dynamicCallableDescriptors.dynamicType)
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
                            .forEach { c.trace.report(Errors.UNSUPPORTED.on(it, "modifier on parameter in function type"))
                    }
                }

                param.valOrVarKeyword?.let {
                    c.trace.report(Errors.UNSUPPORTED.on(it, "val or val on parameter in function type"))
                }
            }
        })

        return result ?: type(ErrorUtils.createErrorType(typeElement?.getDebugText() ?: "No type element"))
    }

    private fun resolveTypeForTypeParameter(
            c: TypeResolutionContext, annotations: Annotations,
            typeParameter: TypeParameterDescriptor,
            referenceExpression: KtSimpleNameExpression,
            type: KtUserType
    ): KotlinType {
        val scopeForTypeParameter = getScopeForTypeParameter(c, typeParameter)

        val arguments = resolveTypeProjections(c, ErrorUtils.createErrorType("No type").constructor, type.typeArguments)
        if (!arguments.isEmpty()) {
            c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), 0))
        }

        val containing = typeParameter.containingDeclaration
        if (containing is ClassDescriptor) {
            DescriptorResolver.checkHasOuterClassInstance(c.scope, c.trace, referenceExpression, containing)
        }

        return if (scopeForTypeParameter is ErrorUtils.ErrorScope)
            ErrorUtils.createErrorType("?")
        else
            KotlinTypeImpl.create(
                    annotations,
                    typeParameter.typeConstructor,
                    false,
                    listOf(),
                    scopeForTypeParameter)
    }

    private fun getScopeForTypeParameter(c: TypeResolutionContext, typeParameterDescriptor: TypeParameterDescriptor): MemberScope {
        return when {
            c.checkBounds -> TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor).memberScope
            else -> LazyScopeAdapter(LockBasedStorageManager.NO_LOCKS.createLazyValue {
                TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor).memberScope
            })
        }
    }

    private fun resolveTypeForClass(
            c: TypeResolutionContext, annotations: Annotations,
            classDescriptor: ClassDescriptor, type: KtUserType,
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
            return createErrorTypeAndResolveArguments(c, projectionFromAllQualifierParts, "[Error type: $typeConstructor]")
        }

        val collectedArgumentAsTypeProjections =
                collectArgumentsForClassTypeConstructor(c, classDescriptor, qualifierResolutionResult.qualifierParts)
                ?: return createErrorTypeAndResolveArguments(c, projectionFromAllQualifierParts, typeConstructor.toString())

        assert(collectedArgumentAsTypeProjections.size <= parameters.size) {
            "Collected arguments count should be not greater then parameters count," +
            " but ${collectedArgumentAsTypeProjections.size} instead of ${parameters.size} found in ${type.text}"
        }

        val argumentsFromUserType = resolveTypeProjections(c, typeConstructor, collectedArgumentAsTypeProjections)
        val arguments = argumentsFromUserType + appendDefaultArgumentsForInnerScope(argumentsFromUserType.size, parameters)

        assert(arguments.size == parameters.size) {
            "Collected arguments count should be equal to parameters count," +
            " but ${collectedArgumentAsTypeProjections.size} instead of ${parameters.size} found in ${type.text}"
        }

        if (Flexibility.FLEXIBLE_TYPE_CLASSIFIER.asSingleFqName().toUnsafe() == DescriptorUtils.getFqName(classDescriptor)
            && parameters.size == 2) {
            // We create flexible types by convention here
            // This is not intended to be used in normal users' environments, only for tests and debugger etc
            return type(DelegatingFlexibleType.create(
                    arguments[0].type,
                    arguments[1].type,
                    flexibleTypeCapabilitiesProvider.getCapabilities())
            )
        }

        val resultingType = KotlinTypeImpl.create(annotations, classDescriptor, false, arguments)
        if (c.checkBounds) {
            val substitutor = TypeSubstitutor.create(resultingType)
            for (i in parameters.indices) {
                val parameter = parameters[i]
                val argument = arguments[i].type
                val typeReference = collectedArgumentAsTypeProjections.getOrNull(i)?.typeReference

                if (typeReference != null) {
                    DescriptorResolver.checkBounds(typeReference, argument, parameter, substitutor, c.trace)
                }
            }
        }

        if (resultingType.isArrayOfNothing()) {
            c.trace.report(UNSUPPORTED.on(type, "Array<Nothing> is illegal"))
        }

        return type(resultingType)
    }

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
    private fun isPossibleToSpecifyTypeArgumentsFor(classDescriptor: ClassDescriptor): Boolean {
        // First parameter relates to the innermost declaration
        // If it's declared in function there
        val firstTypeParameter = classDescriptor.typeConstructor.parameters.firstOrNull() ?: return false
        return firstTypeParameter.original.containingDeclaration is ClassDescriptor
    }

    private fun collectArgumentsForClassTypeConstructor(
            c: TypeResolutionContext,
            classDescriptor: ClassDescriptor,
            qualifierParts: List<QualifiedExpressionResolver.QualifierPart>
    ): List<KtTypeProjection>? {
        val classDescriptorChain = classDescriptor.classDescriptorChain()
        val reversedQualifierParts = qualifierParts.asReversed()

        var wasStatic = false
        var result = SmartList<KtTypeProjection>()

        val classChainLastIndex = Math.min(classDescriptorChain.size, reversedQualifierParts.size) - 1

        for (index in 0..classChainLastIndex) {
            val qualifierPart = reversedQualifierParts[index]
            val currentArguments = qualifierPart.typeArguments?.arguments.orEmpty()
            val declaredTypeParameters = classDescriptorChain[index].declaredTypeParameters
            val currentParameters = if (wasStatic) emptyList() else declaredTypeParameters

            if (wasStatic && currentArguments.isNotEmpty() && declaredTypeParameters.isNotEmpty()) {
                c.trace.report(TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED.on(qualifierPart.typeArguments!!))
                return null
            }

            if (currentArguments.size != currentParameters.size) {
                c.trace.report(
                        WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(qualifierPart.typeArguments ?: qualifierPart.expression, currentParameters.size))
                return null
            }

            result.addAll(currentArguments)

            wasStatic = wasStatic || !classDescriptorChain[index].isInner
        }

        val nonClassQualifierParts =
                reversedQualifierParts.subList(
                        Math.min(classChainLastIndex + 1, reversedQualifierParts.size),
                        reversedQualifierParts.size)

        for (qualifierPart in nonClassQualifierParts) {
            if (qualifierPart.typeArguments != null) {
                c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(qualifierPart.typeArguments, 0))
                return null
            }
        }

        val parameters = classDescriptor.typeConstructor.parameters
        if (result.size < parameters.size) {
            val typeParametersToSpecify =
                    parameters.subList(result.size, parameters.size).takeWhile { it.original.containingDeclaration is ClassDescriptor }
            if (typeParametersToSpecify.any { parameter -> !parameter.isDeclaredInScope(c) }) {
                c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(qualifierParts.last().expression, parameters.size))
                return null
            }
        }

        return result
    }

    private fun ClassifierDescriptor?.classDescriptorChain(): List<ClassDescriptor>
            = sequence({ this as? ClassDescriptor }, { it.containingDeclaration as? ClassDescriptor }).toList()

    private fun TypeParameterDescriptor.isDeclaredInScope(c: TypeResolutionContext): Boolean {
        assert(containingDeclaration is ClassDescriptor) { "This function is implemented for classes only, but $containingDeclaration was given" }

        // This function checks whether this@TypeParameterDescriptor ()is reachable from current scope by it's name
        // The only way it can be is that we are within class that contains it
        // We could just walk through containing declarations as it's done on last return, but it can be rather slow, so we at first look into scope.
        // Latter can fail if we found some other classifier with same name, but parameter can still be reachable, so
        // in case of fail we fall back into slow but exact computation.

        val contributedClassifier = c.scope.findClassifier(name, NoLookupLocation.WHEN_RESOLVING_DEFAULT_TYPE_ARGUMENTS) ?: return false
        if (contributedClassifier.typeConstructor == typeConstructor) return true

        return c.scope.ownerDescriptor.isInsideOfClass(original.containingDeclaration as ClassDescriptor)
    }

    private fun DeclarationDescriptor.isInsideOfClass(classDescriptor: ClassDescriptor)
            = sequence(this, { it.containingDeclaration }).any { it.original == classDescriptor }


    private fun resolveTypeProjectionsWithErrorConstructor(
            c: TypeResolutionContext,
            argumentElements: List<KtTypeProjection>,
            message: String = "Error type for resolving type projections"
    ) = resolveTypeProjections(c, ErrorUtils.createErrorTypeConstructor(message), argumentElements)

    private fun createErrorTypeAndResolveArguments(
            c: TypeResolutionContext,
            argumentElements: List<KtTypeProjection>,
            message: String = ""
    ): PossiblyBareType
        = type(ErrorUtils.createErrorTypeWithArguments(message, resolveTypeProjectionsWithErrorConstructor(c, argumentElements)))

    // In cases like
    // class Outer<F> {
    //      inner class Inner<E>
    //      val inner: Inner<String>
    // }
    //
    // FQ type of 'inner' val is Outer<F>.Inner<String> (saying strictly it's Outer.Inner<String, F>), but 'F' is implicitly came from scope
    // So we just add it explicitly to make type complete, in a sense of having arguments count equal to parameters one.
    private fun appendDefaultArgumentsForInnerScope(
            fromIndex: Int,
            constructorParameters: List<TypeParameterDescriptor>
    ) = constructorParameters.subList(fromIndex, constructorParameters.size).map {
        TypeProjectionImpl((it.original as TypeParameterDescriptor).defaultType)
    }

    private fun resolveTypeProjections(c: TypeResolutionContext, constructor: TypeConstructor, argumentElements: List<KtTypeProjection>): List<TypeProjection> {
        return argumentElements.mapIndexed { i, argumentElement ->

            val projectionKind = argumentElement.getProjectionKind()
            ModifierCheckerCore.check(argumentElement, c.trace, null)
            if (projectionKind == KtProjectionKind.STAR) {
                val parameters = constructor.parameters
                if (parameters.size > i) {
                    val parameterDescriptor = parameters[i]
                    TypeUtils.makeStarProjection(parameterDescriptor)
                }
                else {
                    TypeProjectionImpl(OUT_VARIANCE, ErrorUtils.createErrorType("*"))
                }
            }
            else {
                val type = resolveType(c.noBareTypes(), argumentElement.getTypeReference()!!)
                val kind = resolveProjectionKind(projectionKind)
                if (constructor.parameters.size > i) {
                    val parameterDescriptor = constructor.parameters[i]
                    if (kind != INVARIANT && parameterDescriptor.variance != INVARIANT) {
                        if (kind == parameterDescriptor.variance) {
                            c.trace.report(REDUNDANT_PROJECTION.on(argumentElement, constructor.getDeclarationDescriptor()))
                        }
                        else {
                            c.trace.report(CONFLICTING_PROJECTION.on(argumentElement, constructor.getDeclarationDescriptor()))
                        }
                    }
                }
                TypeProjectionImpl(kind, type)
            }

        }
    }

    fun resolveClass(
            scope: LexicalScope, userType: KtUserType, trace: BindingTrace
    ): ClassifierDescriptor? = resolveDescriptorForType(scope, userType, trace).classifierDescriptor

    fun resolveDescriptorForType(
            scope: LexicalScope, userType: KtUserType, trace: BindingTrace
    ): QualifiedExpressionResolver.TypeQualifierResolutionResult {
        if (userType.qualifier != null) { // we must resolve all type references in arguments of qualifier type
            for (typeArgument in userType.qualifier!!.typeArguments) {
                typeArgument.typeReference?.let {
                    forceResolveTypeContents(resolveType(scope, it, trace, true))
                }
            }
        }

        val result = qualifiedExpressionResolver.resolveDescriptorForType(userType, scope, trace)
        if (result.classifierDescriptor != null) {
            PlatformTypesMappedToKotlinChecker.reportPlatformClassMappedToKotlin(
                    moduleDescriptor, trace, userType, result.classifierDescriptor)
        }
        return result
    }

    companion object {
        @JvmStatic fun resolveProjectionKind(projectionKind: KtProjectionKind): Variance {
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
