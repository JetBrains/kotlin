/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
class TypeTranslator(
    private val symbolTable: ReferenceSymbolTable,
    val languageVersionSettings: LanguageVersionSettings,
    builtIns: KotlinBuiltIns,
    private val typeParametersResolver: TypeParametersResolver = ScopedTypeParametersResolver(),
    private val enterTableScope: Boolean = false,
    private val extensions: StubGeneratorExtensions = StubGeneratorExtensions.EMPTY
) {

    private val erasureStack = Stack<PropertyDescriptor>()

    private val typeApproximatorForNI = TypeApproximator(builtIns)
    lateinit var constantValueGenerator: ConstantValueGenerator

    fun enterScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.enterTypeParameterScope(irElement)
        if (enterTableScope) {
            symbolTable.enterScope(irElement)
        }
    }

    fun leaveScope(irElement: IrTypeParametersContainer) {
        typeParametersResolver.leaveTypeParameterScope()
        if (enterTableScope) {
            symbolTable.leaveScope(irElement)
        }
    }

    fun <T> withTypeErasure(propertyDescriptor: PropertyDescriptor, b: () -> T): T {
        try {
            erasureStack.push(propertyDescriptor)
            return b()
        } finally {
            erasureStack.pop()
        }
    }

    inline fun <T> buildWithScope(container: IrTypeParametersContainer, builder: () -> T): T {
        enterScope(container)
        val result = builder()
        leaveScope(container)
        return result
    }

    private fun resolveTypeParameter(typeParameterDescriptor: TypeParameterDescriptor): IrTypeParameterSymbol {
        val originalTypeParameter = typeParameterDescriptor.originalTypeParameter
        return typeParametersResolver.resolveScopedTypeParameter(originalTypeParameter)
            ?: symbolTable.referenceTypeParameter(originalTypeParameter)
    }

    fun translateType(kotlinType: KotlinType): IrType =
        translateType(kotlinType, Variance.INVARIANT).type

    private fun translateType(kotlinType: KotlinType, variance: Variance): IrTypeProjection {
        val approximatedType = approximate(kotlinType)

        when {
            approximatedType.isError ->
                return IrErrorTypeImpl(approximatedType, translateTypeAnnotations(approximatedType), variance)
            approximatedType.isDynamic() ->
                return IrDynamicTypeImpl(approximatedType, translateTypeAnnotations(approximatedType), variance)
        }

        val upperType = approximatedType.upperIfFlexible()
        val upperTypeDescriptor = upperType.constructor.declarationDescriptor
            ?: throw AssertionError("No descriptor for type $upperType")

        if (erasureStack.isNotEmpty()) {
            if (upperTypeDescriptor is TypeParameterDescriptor) {
                if (upperTypeDescriptor.containingDeclaration in erasureStack) {
                    // This hack is about type parameter leak in case of generic delegated property
                    // Such code has to be prohibited since LV 1.5
                    // For more details see commit message or KT-24643
                    return approximateUpperBounds(upperTypeDescriptor.upperBounds, variance)
                }
            }
        }

        return IrSimpleTypeBuilder().apply {
            this.kotlinType = approximatedType
            this.hasQuestionMark = upperType.isMarkedNullable
            this.variance = variance
            this.abbreviation = upperType.getAbbreviation()?.toIrTypeAbbreviation()

            when (upperTypeDescriptor) {
                is TypeParameterDescriptor -> {
                    classifier = resolveTypeParameter(upperTypeDescriptor)
                    annotations = translateTypeAnnotations(upperType, approximatedType)
                }

                is ScriptDescriptor -> {
                    classifier = symbolTable.referenceScript(upperTypeDescriptor)
                }
                is ClassDescriptor -> {
                    // Types such as 'java.util.Collection<? extends CharSequence>' are treated as
                    // '( kotlin.collections.MutableCollection<out kotlin.CharSequence!>
                    //   .. kotlin.collections.Collection<kotlin.CharSequence!>? )'
                    // by the front-end.
                    // When generating generic signatures, JVM BE uses generic arguments of lower bound,
                    // thus producing 'java.util.Collection<? extends CharSequence>' from
                    // 'kotlin.collections.MutableCollection<out kotlin.CharSequence!>'.
                    // Construct equivalent type here.
                    // NB the difference is observed only when lowerTypeDescriptor != upperTypeDescriptor,
                    // which corresponds to mutability-flexible types such as mentioned above.
                    val lowerType = approximatedType.lowerIfFlexible()
                    val lowerTypeDescriptor =
                        lowerType.constructor.declarationDescriptor as? ClassDescriptor
                            ?: throw AssertionError("No class descriptor for lower type $lowerType of $approximatedType")
                    classifier = symbolTable.referenceClass(lowerTypeDescriptor)
                    arguments = when {
                        approximatedType is RawType ->
                            translateTypeArguments(approximatedType.arguments)
                        lowerTypeDescriptor != upperTypeDescriptor ->
                            translateTypeArguments(lowerType.arguments)
                        else ->
                            translateTypeArguments(upperType.arguments)
                    }
                    annotations = translateTypeAnnotations(upperType, approximatedType)
                }

                else ->
                    throw AssertionError("Unexpected type descriptor $upperTypeDescriptor :: ${upperTypeDescriptor::class}")
            }
        }.buildTypeProjection()
    }

    private fun approximateUpperBounds(upperBounds: Collection<KotlinType>, variance: Variance): IrTypeProjection {
        val commonSupertype = CommonSupertypes.commonSupertype(upperBounds)
        return translateType(approximate(commonSupertype.replaceArgumentsWithStarProjections()), variance)
    }

    private fun SimpleType.toIrTypeAbbreviation(): IrTypeAbbreviation {
        val typeAliasDescriptor = constructor.declarationDescriptor.let {
            it as? TypeAliasDescriptor
                ?: throw AssertionError("TypeAliasDescriptor expected: $it")
        }
        return IrTypeAbbreviationImpl(
            symbolTable.referenceTypeAlias(typeAliasDescriptor),
            isMarkedNullable,
            translateTypeArguments(this.arguments),
            translateTypeAnnotations(this)
        )
    }

    fun approximate(ktType: KotlinType): KotlinType {
        val properlyApproximatedType = approximateByKotlinRules(ktType)

        // If there's an intersection type, take the most common supertype of its intermediate supertypes.
        // That's what old back-end effectively does.
        val typeConstructor = properlyApproximatedType.constructor
        if (typeConstructor is IntersectionTypeConstructor) {
            val commonSupertype = CommonSupertypes.commonSupertype(typeConstructor.supertypes)
            return approximate(commonSupertype.replaceArgumentsWithStarProjections())
        }

        // Assume that other types are approximated properly.
        return properlyApproximatedType
    }

    private val isWithNewInference = languageVersionSettings.supportsFeature(LanguageFeature.NewInference)

    private fun approximateByKotlinRules(ktType: KotlinType): KotlinType =
        if (isWithNewInference) {
            if (ktType.constructor.isDenotable && ktType.arguments.isEmpty())
                ktType
            else
                typeApproximatorForNI.approximateDeclarationType(
                    ktType,
                    local = false,
                    languageVersionSettings = languageVersionSettings
                )
        } else {
            // Hack to preserve *-projections in arguments in OI.
            // Expected to be removed as soon as OI is deprecated.
            if (ktType.constructor.isDenotable)
                ktType
            else
                approximateCapturedTypes(ktType).upper
        }

    private fun translateTypeAnnotations(kotlinType: KotlinType, flexibleType: KotlinType = kotlinType): List<IrConstructorCall> {
        val annotations = kotlinType.annotations
        val irAnnotations = ArrayList<IrConstructorCall>()

        annotations.mapNotNullTo(irAnnotations) {
            constantValueGenerator.generateAnnotationConstructorCall(it)
        }

        // EnhancedNullability annotation is not present in 'annotations', see 'EnhancedTypeAnnotations::iterator()'.
        // Also, EnhancedTypeAnnotationDescriptor is not a "real" annotation descriptor, there's no corresponding ClassDescriptor, etc.
        if (extensions.enhancedNullability.hasEnhancedNullability(kotlinType)) {
            irAnnotations.addSpecialAnnotation(extensions.enhancedNullabilityAnnotationConstructor)
        }

        if (flexibleType.isNullabilityFlexible()) {
            irAnnotations.addSpecialAnnotation(extensions.flexibleNullabilityAnnotationConstructor)
        }

        if (flexibleType is RawType) {
            irAnnotations.addSpecialAnnotation(extensions.rawTypeAnnotationConstructor)
        }

        return irAnnotations
    }

    private fun MutableList<IrConstructorCall>.addSpecialAnnotation(irConstructor: IrConstructor?) {
        if (irConstructor != null) {
            add(
                IrConstructorCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    irConstructor.constructedClassType,
                    irConstructor.symbol
                )
            )
        }
    }

    private fun translateTypeArguments(arguments: List<TypeProjection>) =
        arguments.map {
            if (it.isStarProjection)
                IrStarProjectionImpl
            else
                translateType(it.type, it.projectionKind)
        }
}
