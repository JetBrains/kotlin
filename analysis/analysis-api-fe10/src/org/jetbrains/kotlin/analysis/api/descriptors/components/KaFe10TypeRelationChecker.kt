/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.IsErrorTypeEqualToAnythingTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl
import org.jetbrains.kotlin.types.lowerIfFlexible

internal class KaFe10TypeRelationChecker(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseTypeRelationChecker<KaFe10Session>(), KaFe10SessionComponent {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        require(this is KaFe10Type)
        require(other is KaFe10Type)
        return getTypeCheckerFor(errorTypePolicy).equalTypes(this.fe10Type, other.fe10Type)
    }

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        require(this is KaFe10Type)
        require(supertype is KaFe10Type)
        return getTypeCheckerFor(errorTypePolicy).isSubtypeOf(this.fe10Type, supertype.fe10Type)
    }

    override fun KaType.isClassSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        val superclassDescriptor = analysisContext.resolveSession.moduleDescriptor.findClassifierAcrossModuleDependencies(classId)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return isClassSubtypeOf(superclassDescriptor, errorTypePolicy)
    }

    override fun KaType.isClassSubtypeOf(
        symbol: KaClassLikeSymbol,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean {
        val superclassDescriptor = getSymbolDescriptor(symbol) as? ClassifierDescriptor ?: return false
        return isClassSubtypeOf(superclassDescriptor, errorTypePolicy)
    }

    @OptIn(TypeRefinement::class)
    private fun KaType.isClassSubtypeOf(
        superclassDescriptor: ClassifierDescriptor,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean {
        require(this is KaFe10Type)

        // We have to refine and prepare the type to be in line with `equalTypes` and `isSubtypeOf`.
        val typeChecker = analysisContext.resolveSession.kotlinTypeCheckerOfOwnerModule
        val preparedType = typeChecker.kotlinTypePreparator.prepareType(typeChecker.kotlinTypeRefiner.refineType(fe10Type))

        val classDescriptor = preparedType.lowerIfFlexible().constructor.declarationDescriptor as? ClassDescriptor

        val expandedSuperclassDescriptor = when (superclassDescriptor) {
            is ClassDescriptor -> superclassDescriptor
            is TypeAliasDescriptor -> superclassDescriptor.classDescriptor ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT
            else -> return false
        }

        // If the left-hand side is not a class type, we have to fall back to full subtyping. For example, a type parameter
        // `T : List<String>` would still be a subtype of `Iterable<*>`, as would an intersection type `Interface & List<String>`.
        if (classDescriptor == null) {
            val typeParameters = superclassDescriptor.typeConstructor.parameters
            val projections = typeParameters.map { StarProjectionImpl(it) }
            val superclassType = TypeUtils.substituteProjectionsForParameters(expandedSuperclassDescriptor, projections)

            return getTypeCheckerFor(errorTypePolicy).isSubtypeOf(fe10Type, superclassType)
        }

        return classDescriptor.isSubclassOf(expandedSuperclassDescriptor)
    }

    private fun getTypeCheckerFor(errorTypePolicy: KaSubtypingErrorTypePolicy): KotlinTypeChecker {
        val typeChecker = analysisContext.resolveSession.kotlinTypeCheckerOfOwnerModule
        if (typeChecker !is NewKotlinTypeCheckerImpl) return typeChecker

        // `NewKotlinTypeCheckerImpl` is inconsistent with its error type leniency: `isSubtypeOf` is lenient by default while `equalTypes`
        // isn't. Hence, even without a `LENIENT` policy, we need to wrap `typeChecker` to achieve consistent strictness.
        return IsErrorTypeEqualToAnythingTypeChecker(typeChecker, errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT)
    }
}
