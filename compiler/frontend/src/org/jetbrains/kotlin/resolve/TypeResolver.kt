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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.LazyScopeAdapter
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.storage.LockBasedStorageManager

import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.resolve.PossiblyBareType.type
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.resolve.TypeResolver.FlexibleTypeCapabilitiesProvider
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.context.LazinessToken
import org.jetbrains.kotlin.resolve.lazy.LazyEntity
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.codeFragmentUtil.debugTypeInfo

public class TypeResolver(
        private val annotationResolver: AnnotationResolver,
        private val qualifiedExpressionResolver: QualifiedExpressionResolver,
        private val moduleDescriptor: ModuleDescriptor,
        private val flexibleTypeCapabilitiesProvider: FlexibleTypeCapabilitiesProvider,
        private val storageManager: StorageManager,
        private val lazinessToken: LazinessToken,
        private val dynamicTypesSettings: DynamicTypesSettings
) {

    public open class FlexibleTypeCapabilitiesProvider {
        public open fun getCapabilities(): FlexibleTypeCapabilities {
            return FlexibleTypeCapabilities.NONE
        }
    }

    public fun resolveType(scope: JetScope, typeReference: JetTypeReference, trace: BindingTrace, checkBounds: Boolean): JetType {
        // bare types are not allowed
        return resolveType(TypeResolutionContext(scope, trace, checkBounds, false), typeReference)
    }

    private fun resolveType(c: TypeResolutionContext, typeReference: JetTypeReference): JetType {
        assert(!c.allowBareTypes, "Use resolvePossiblyBareType() when bare types are allowed")
        return resolvePossiblyBareType(c, typeReference).getActualType()
    }

    public fun resolvePossiblyBareType(c: TypeResolutionContext, typeReference: JetTypeReference): PossiblyBareType {
        val cachedType = c.trace.getBindingContext().get(BindingContext.TYPE, typeReference)
        if (cachedType != null) return type(cachedType)

        if (typeReference.debugTypeInfo != null) {
            val debugType = typeReference.debugTypeInfo
            c.trace.record(BindingContext.TYPE, typeReference, debugType)
            return type(debugType)
        }

        if (!c.allowBareTypes && lazinessToken.isLazy()) {
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

    private fun doResolvePossiblyBareType(c: TypeResolutionContext, typeReference: JetTypeReference): PossiblyBareType {
        val annotations = annotationResolver.resolveAnnotationsWithArguments(c.scope, typeReference.getAnnotations(), c.trace)

        val typeElement = typeReference.getTypeElement()

        val type = resolveTypeElement(c, annotations, typeElement)
        c.trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, typeReference, c.scope)

        return type
    }

    private fun resolveTypeElement(c: TypeResolutionContext, annotations: Annotations, typeElement: JetTypeElement?): PossiblyBareType {
        var result: PossiblyBareType? = null
        typeElement?.accept(object : JetVisitorVoid() {
            override fun visitUserType(type: JetUserType) {
                val referenceExpression = type.getReferenceExpression()
                val referencedName = type.getReferencedName()
                if (referenceExpression == null || referencedName == null) return

                val classifierDescriptor = resolveClass(c.scope, type, c.trace)
                if (classifierDescriptor == null) {
                    resolveTypeProjections(c, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments())
                    return
                }

                c.trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor)

                when (classifierDescriptor) {
                    is TypeParameterDescriptor -> {

                        val scopeForTypeParameter = getScopeForTypeParameter(c, classifierDescriptor)
                        result = if (scopeForTypeParameter is ErrorUtils.ErrorScope)
                                    type(ErrorUtils.createErrorType("?"))
                                 else
                                    type(JetTypeImpl(
                                            annotations,
                                            classifierDescriptor.getTypeConstructor(),
                                            TypeUtils.hasNullableLowerBound(classifierDescriptor),
                                            listOf(),
                                            scopeForTypeParameter)
                                    )

                        val arguments = resolveTypeProjections(c, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments())
                        if (!arguments.isEmpty()) {
                            c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), 0))
                        }

                        val containing = classifierDescriptor.getContainingDeclaration()
                        if (containing is ClassDescriptor) {
                            DescriptorResolver.checkHasOuterClassInstance(c.scope, c.trace, referenceExpression, containing)
                        }
                    }
                    is ClassDescriptor -> {
                        val typeConstructor = classifierDescriptor.getTypeConstructor()
                        val arguments = resolveTypeProjections(c, typeConstructor, type.getTypeArguments())
                        val parameters = typeConstructor.getParameters()
                        val expectedArgumentCount = parameters.size()
                        val actualArgumentCount = arguments.size()
                        if (ErrorUtils.isError(classifierDescriptor)) {
                            result = type(ErrorUtils.createErrorType("[Error type: " + typeConstructor + "]"))
                        }
                        else {
                            if (actualArgumentCount != expectedArgumentCount) {
                                if (actualArgumentCount == 0) {
                                    // See docs for PossiblyBareType
                                    if (c.allowBareTypes) {
                                        result = PossiblyBareType.bare(typeConstructor, false)
                                        return
                                    }
                                    c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type, expectedArgumentCount))
                                }
                                else {
                                    c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), expectedArgumentCount))
                                }
                                result = type(ErrorUtils.createErrorTypeWithArguments("" + typeConstructor, arguments))
                            }
                            else {
                                if (Flexibility.FLEXIBLE_TYPE_CLASSIFIER.asSingleFqName() == DescriptorUtils.getFqName(classifierDescriptor)
                                    && classifierDescriptor.getTypeConstructor().getParameters().size() == 2) {
                                    // We create flexible types by convention here
                                    // This is not intended to be used in normal users' environments, only for tests and debugger etc
                                    result = type(DelegatingFlexibleType.create(
                                            arguments[0].getType(),
                                            arguments[1].getType(),
                                            flexibleTypeCapabilitiesProvider.getCapabilities())
                                    )
                                    return
                                }
                                val resultingType = JetTypeImpl(annotations, typeConstructor, false, arguments, classifierDescriptor.getMemberScope(arguments))
                                result = type(resultingType)
                                if (c.checkBounds) {
                                    val substitutor = TypeSubstitutor.create(resultingType)
                                    for (i in parameters.indices) {
                                        val parameter = parameters[i]
                                        val argument = arguments[i].getType()
                                        val typeReference = type.getTypeArguments()[i].getTypeReference()

                                        if (typeReference != null) {
                                            DescriptorResolver.checkBounds(typeReference, argument, parameter, substitutor, c.trace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun visitNullableType(nullableType: JetNullableType) {
                val innerType = nullableType.getInnerType()
                val baseType = resolveTypeElement(c, annotations, innerType)
                if (baseType.isNullable() || innerType is JetNullableType || innerType is JetDynamicType) {
                    c.trace.report(REDUNDANT_NULLABLE.on(nullableType))
                }
                else if (!baseType.isBare() && TypeUtils.hasNullableSuperType(baseType.getActualType())) {
                    c.trace.report(BASE_WITH_NULLABLE_UPPER_BOUND.on(nullableType, baseType.getActualType()))
                }
                result = baseType.makeNullable()
            }

            override fun visitFunctionType(type: JetFunctionType) {
                val receiverTypeRef = type.getReceiverTypeReference()
                val receiverType = if (receiverTypeRef == null) null else resolveType(c.noBareTypes(), receiverTypeRef)

                val parameterTypes = type.getParameters().map { resolveType(c.noBareTypes(), it.getTypeReference()) }

                val returnTypeRef = type.getReturnTypeReference()
                val returnType = if (returnTypeRef != null)
                                     resolveType(c.noBareTypes(), returnTypeRef)
                                 else KotlinBuiltIns.getInstance().getUnitType()
                result = type(KotlinBuiltIns.getInstance().getFunctionType(annotations, receiverType, parameterTypes, returnType))
            }

            override fun visitDynamicType(type: JetDynamicType) {
                result = type(DynamicType)
                if (!dynamicTypesSettings.dynamicTypesAllowed) {
                    c.trace.report(UNSUPPORTED.on(type, "Dynamic types are not supported in this context"))
                }
            }

            override fun visitSelfType(type: JetSelfType) {
                c.trace.report(UNSUPPORTED.on(type, "Self-types are not supported"))
            }

            override fun visitJetElement(element: JetElement) {
                c.trace.report(UNSUPPORTED.on(element, "Self-types are not supported yet"))
            }
        })
        return result ?: type(ErrorUtils.createErrorType(typeElement?.getDebugText() ?: "No type element"))
    }

    private fun getScopeForTypeParameter(c: TypeResolutionContext, typeParameterDescriptor: TypeParameterDescriptor): JetScope {
        if (c.checkBounds) {
            return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope()
        }
        else {
            return LazyScopeAdapter(LockBasedStorageManager.NO_LOCKS.createLazyValue {
                    typeParameterDescriptor.getUpperBoundsAsType().getMemberScope()
            })
        }
    }

    private fun resolveTypeProjections(c: TypeResolutionContext, constructor: TypeConstructor, argumentElements: List<JetTypeProjection>): List<TypeProjection> {
        return argumentElements.withIndices().map {
            val (i, argumentElement) = it

            val projectionKind = argumentElement.getProjectionKind()
            ModifiersChecker.checkIncompatibleVarianceModifiers(argumentElement.getModifierList(), c.trace)
            if (projectionKind == JetProjectionKind.STAR) {
                val parameters = constructor.getParameters()
                if (parameters.size() > i) {
                    val parameterDescriptor = parameters[i]
                    TypeUtils.makeStarProjection(parameterDescriptor)
                }
                else {
                    TypeProjectionImpl(OUT_VARIANCE, ErrorUtils.createErrorType("*"))
                }
            }
            else {
                // TODO : handle the Foo<in *> case
                val type = resolveType(c.noBareTypes(), argumentElement.getTypeReference())
                val kind = resolveProjectionKind(projectionKind)
                if (constructor.getParameters().size() > i) {
                    val parameterDescriptor = constructor.getParameters()[i]
                    if (kind != INVARIANT && parameterDescriptor.getVariance() != INVARIANT) {
                        if (kind == parameterDescriptor.getVariance()) {
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

    public fun resolveClass(scope: JetScope, userType: JetUserType, trace: BindingTrace): ClassifierDescriptor? {
        val classifierDescriptor = qualifiedExpressionResolver.lookupDescriptorsForUserType(userType, scope, trace, true)
                                        .firstIsInstanceOrNull<ClassifierDescriptor>()
        if (classifierDescriptor != null) {
            PlatformTypesMappedToKotlinChecker.reportPlatformClassMappedToKotlin(moduleDescriptor, trace, userType, classifierDescriptor)
        }
        return classifierDescriptor
    }

    class object {
        [platformStatic]
        public fun resolveProjectionKind(projectionKind: JetProjectionKind): Variance {
            return when (projectionKind) {
                JetProjectionKind.IN -> IN_VARIANCE
                JetProjectionKind.OUT -> OUT_VARIANCE
                JetProjectionKind.NONE -> INVARIANT
                else -> // NOTE: Star projections must be handled before this method is called
                    throw IllegalStateException("Illegal projection kind:" + projectionKind)
            }
        }
    }
}
