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

package org.jetbrains.kotlin.load.java.typeEnhancement

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.computeNewDefaultTypeQualifiers
import org.jetbrains.kotlin.load.java.lazy.descriptors.isJavaField
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

class SignatureEnhancement(private val annotationTypeQualifierResolver: AnnotationTypeQualifierResolver) {

    fun extractNullability(annotationDescriptor: AnnotationDescriptor): NullabilityQualifier? {
        val annotationFqName = annotationDescriptor.fqName ?: return null
        when (annotationFqName) {
            in NULLABLE_ANNOTATIONS -> return NullabilityQualifier.NULLABLE
            in NOT_NULL_ANNOTATIONS -> return NullabilityQualifier.NOT_NULL
        }

        val typeQualifier =
                when {
                    annotationFqName == JAVAX_NONNULL_ANNOTATION -> annotationDescriptor
                    else -> annotationTypeQualifierResolver.resolveTypeQualifierAnnotation(annotationDescriptor)
                            ?.takeIf { it.fqName == JAVAX_NONNULL_ANNOTATION }
                } ?: return null

        val enumEntryDescriptor =
                typeQualifier.allValueArguments.values.singleOrNull()?.value
                // if no argument is specified, use default value: NOT_NULL
                ?: return NullabilityQualifier.NOT_NULL

        if (enumEntryDescriptor !is ClassDescriptor) return null

        return when (enumEntryDescriptor.name.asString()) {
            "ALWAYS" -> NullabilityQualifier.NOT_NULL
            "MAYBE" -> NullabilityQualifier.NULLABLE
            else -> null
        }
    }


    fun <D : CallableMemberDescriptor> enhanceSignatures(c: LazyJavaResolverContext, platformSignatures: Collection<D>): Collection<D> {
        return platformSignatures.map {
            it.enhanceSignature(c)
        }
    }

    private fun <D : CallableMemberDescriptor> D.enhanceSignature(c: LazyJavaResolverContext): D {
        val outerScopeQualifiers = c.computeNewDefaultTypeQualifiers(annotations)
        // TODO type parameters
        // TODO use new type parameters while enhancing other types
        // TODO Propagation into generic type arguments

        if (this !is JavaCallableMemberDescriptor) return this

        // Fake overrides with one overridden has been enhanced before
        if (kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE && original.overriddenDescriptors.size == 1) return this

        val receiverTypeEnhancement =
                if (extensionReceiverParameter != null)
                    parts(
                            typeContainer =
                                this.safeAs<FunctionDescriptor>()
                                    ?.getUserData(JavaMethodDescriptor.ORIGINAL_VALUE_PARAMETER_FOR_EXTENSION_RECEIVER),
                            isCovariant = false,
                            defaultTopLevelQualifiers =
                                outerScopeQualifiers
                                        ?.get(AnnotationTypeQualifierResolver.QualifierApplicabilityType.VALUE_PARAMETER)
                    ) { it.extensionReceiverParameter!!.type }.enhance()
                else null


        val predefinedEnhancementInfo =
                (this as? JavaMethodDescriptor)
                        ?.run { SignatureBuildingComponents.signature(this.containingDeclaration as ClassDescriptor, this.computeJvmDescriptor()) }
                        ?.let { signature -> PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[signature] }


        predefinedEnhancementInfo?.let {
            assert(it.parametersInfo.size == valueParameters.size) {
                "Predefined enhancement info for $this has ${it.parametersInfo.size}, but ${valueParameters.size} expected"
            }
        }

        val valueParameterEnhancements = valueParameters.map {
            p ->
            parts(
                    typeContainer = p, isCovariant = false,
                    defaultTopLevelQualifiers =
                            outerScopeQualifiers
                                    ?.get(AnnotationTypeQualifierResolver.QualifierApplicabilityType.VALUE_PARAMETER)
            ) { it.valueParameters[p.index].type }
                    .enhance(predefinedEnhancementInfo?.parametersInfo?.getOrNull(p.index))
        }

        val returnTypeEnhancement =
                parts(
                        typeContainer = this, isCovariant = true,
                        defaultTopLevelQualifiers =
                            outerScopeQualifiers?.get(
                                    if (this.safeAs<PropertyDescriptor>()?.isJavaField == true)
                                        AnnotationTypeQualifierResolver.QualifierApplicabilityType.FIELD
                                    else
                                        AnnotationTypeQualifierResolver.QualifierApplicabilityType.METHOD_RETURN_TYPE
                            )


                ) { it.returnType!! }.enhance(predefinedEnhancementInfo?.returnTypeInfo)

        if ((receiverTypeEnhancement?.wereChanges ?: false)
            || returnTypeEnhancement.wereChanges || valueParameterEnhancements.any { it.wereChanges }) {
            @Suppress("UNCHECKED_CAST")
            return this.enhance(receiverTypeEnhancement?.type, valueParameterEnhancements.map { it.type }, returnTypeEnhancement.type) as D
        }

        return this
    }

    private inner class SignatureParts(
            private val typeContainer: Annotated?,
            private val fromOverride: KotlinType,
            private val fromOverridden: Collection<KotlinType>,
            private val isCovariant: Boolean,
            private val defaultTopLevelQualifiers: JavaTypeQualifiers?
    ) {
        fun enhance(predefined: TypeEnhancementInfo? = null): PartEnhancementResult {
            val qualifiers = computeIndexedQualifiersForOverride()

            val qualifiersWithPredefined: ((Int) -> JavaTypeQualifiers)? = predefined?.let {
                {
                    index ->
                    predefined.map[index] ?: qualifiers(index)
                }
            }

            return fromOverride.enhance(qualifiersWithPredefined ?: qualifiers)?.let {
                enhanced ->
                PartEnhancementResult(enhanced, wereChanges = true)
            } ?: PartEnhancementResult(fromOverride, wereChanges = false)
        }

        private fun KotlinType.extractQualifiers(): JavaTypeQualifiers {
            val (lower, upper) =
                    if (this.isFlexible())
                        asFlexibleType().let { Pair(it.lowerBound, it.upperBound) }
                    else Pair(this, this)

            val mapping = JavaToKotlinClassMap
            return JavaTypeQualifiers(
                    when {
                        lower.isMarkedNullable -> NullabilityQualifier.NULLABLE
                        !upper.isMarkedNullable -> NullabilityQualifier.NOT_NULL
                        else -> null
                    },
                    when {
                        mapping.isReadOnly(lower) -> MutabilityQualifier.READ_ONLY
                        mapping.isMutable(upper) -> MutabilityQualifier.MUTABLE
                        else -> null
                    },
                    isNotNullTypeParameter = unwrap() is NotNullTypeParameter)
        }

        private fun KotlinType.extractQualifiersFromAnnotations(isHeadTypeConstructor: Boolean): JavaTypeQualifiers {
            val composedAnnotation =
                    if (isHeadTypeConstructor && typeContainer != null)
                        composeAnnotations(typeContainer.annotations, annotations)
                    else
                        annotations

            fun <T: Any> List<FqName>.ifPresent(qualifier: T) =
                    if (any { composedAnnotation.findAnnotation(it) != null }) qualifier else null

            fun <T: Any> uniqueNotNull(x: T?, y: T?) = if (x == null || y == null || x == y) x ?: y else null

            val defaultTypeQualifier = defaultTopLevelQualifiers?.takeIf { isHeadTypeConstructor }
            val nullability =
                    composedAnnotation.extractNullability()
                    ?: defaultTypeQualifier?.nullability

            return JavaTypeQualifiers(
                    nullability,
                    uniqueNotNull(
                            READ_ONLY_ANNOTATIONS.ifPresent(
                                    MutabilityQualifier.READ_ONLY
                            ),
                            MUTABLE_ANNOTATIONS.ifPresent(
                                    MutabilityQualifier.MUTABLE
                            )
                    ),
                    isNotNullTypeParameter = nullability == NullabilityQualifier.NOT_NULL && isTypeParameter()
            )
        }

        private fun Annotations.extractNullability(): NullabilityQualifier? =
                this.firstNotNullResult(this@SignatureEnhancement::extractNullability)

        private fun computeIndexedQualifiersForOverride(): (Int) -> JavaTypeQualifiers {
            fun KotlinType.toIndexed(): List<KotlinType> {
                val list = ArrayList<KotlinType>(1)

                fun add(type: KotlinType) {
                    list.add(type)
                    for (arg in type.arguments) {
                        if (arg.isStarProjection) {
                            list.add(arg.type)
                        }
                        else {
                            add(arg.type)
                        }
                    }
                }

                add(this)
                return list
            }

            val indexedFromSupertypes = fromOverridden.map { it.toIndexed() }
            val indexedThisType = fromOverride.toIndexed()

            // The covariant case may be hard, e.g. in the superclass the return may be Super<T>, but in the subclass it may be Derived, which
            // is declared to extend Super<T>, and propagating data here is highly non-trivial, so we only look at the head type constructor
            // (outermost type), unless the type in the subclass is interchangeable with the all the types in superclasses:
            // e.g. we have (Mutable)List<String!>! in the subclass and { List<String!>, (Mutable)List<String>! } from superclasses
            // Note that `this` is flexible here, so it's equal to it's bounds
            val onlyHeadTypeConstructor = isCovariant && fromOverridden.any { !KotlinTypeChecker.DEFAULT.equalTypes(it, fromOverride) }

            val treeSize = if (onlyHeadTypeConstructor) 1 else indexedThisType.size
            val computedResult = Array(treeSize) {
                index ->
                val isHeadTypeConstructor = index == 0
                assert(isHeadTypeConstructor || !onlyHeadTypeConstructor) { "Only head type constructors should be computed" }

                val qualifiers = indexedThisType[index]
                val verticalSlice = indexedFromSupertypes.mapNotNull { it.getOrNull(index) }

                // Only the head type constructor is safely co-variant
                qualifiers.computeQualifiersForOverride(verticalSlice, isCovariant && isHeadTypeConstructor, isHeadTypeConstructor)
            }

            return { index -> computedResult.getOrElse(index) { JavaTypeQualifiers.NONE } }
        }

        private fun KotlinType.computeQualifiersForOverride(
                fromSupertypes: Collection<KotlinType>, isCovariant: Boolean,
                isHeadTypeConstructor: Boolean
        ): JavaTypeQualifiers {
            val nullabilityFromSupertypes = fromSupertypes.mapNotNull { it.extractQualifiers().nullability }.toSet()
            val mutabilityFromSupertypes = fromSupertypes.mapNotNull { it.extractQualifiers().mutability }.toSet()

            val own = extractQualifiersFromAnnotations(isHeadTypeConstructor)

            val isAnyNonNullTypeParameter = own.isNotNullTypeParameter || fromSupertypes.any { it.extractQualifiers().isNotNullTypeParameter }

            fun createJavaTypeQualifiers(nullability: NullabilityQualifier?, mutability: MutabilityQualifier?): JavaTypeQualifiers {
                if (!isAnyNonNullTypeParameter || nullability != NullabilityQualifier.NOT_NULL) {
                    return JavaTypeQualifiers(nullability, mutability, false)
                }
                return JavaTypeQualifiers(
                        nullability, mutability,
                        isNotNullTypeParameter = true)
            }

            if (isCovariant) {
                fun <T : Any> Set<T>.selectCovariantly(low: T, high: T, own: T?): T? {
                    val supertypeQualifier = if (low in this) low else if (high in this) high else null
                    return if (supertypeQualifier == low && own == high) null else own ?: supertypeQualifier
                }
                return createJavaTypeQualifiers(
                        nullabilityFromSupertypes.selectCovariantly(
                                NullabilityQualifier.NOT_NULL, NullabilityQualifier.NULLABLE, own.nullability
                        ),
                        mutabilityFromSupertypes.selectCovariantly(
                                MutabilityQualifier.MUTABLE, MutabilityQualifier.READ_ONLY, own.mutability
                        )
                )
            }
            else {
                fun <T : Any> Set<T>.selectInvariantly(own: T?): T? {
                    val effectiveSet = own?.let { (this + own).toSet() } ?: this
                    // if this set contains exactly one element, it is the qualifier everybody agrees upon,
                    // otherwise (no qualifiers, or multiple qualifiers), there's no single such qualifier
                    // and all qualifiers are discarded
                    return effectiveSet.singleOrNull()
                }
                return createJavaTypeQualifiers(
                        nullabilityFromSupertypes.selectInvariantly(own.nullability),
                        mutabilityFromSupertypes.selectInvariantly(own.mutability)
                )
            }
        }
    }

    private data class PartEnhancementResult(val type: KotlinType, val wereChanges: Boolean)

    private fun <D : CallableMemberDescriptor> D.parts(
            typeContainer: Annotated?,
            isCovariant: Boolean,
            defaultTopLevelQualifiers: JavaTypeQualifiers?,
            collector: (CallableMemberDescriptor) -> KotlinType
    ): SignatureParts {
        return SignatureParts(
                typeContainer,
                collector(this),
                this.overriddenDescriptors.map {
                    collector(it)
                },
                isCovariant,
                defaultTopLevelQualifiers
        )
    }

}
