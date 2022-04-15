/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypePreparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.utils.addIfNotNull

object JavaOverrideWithWrongNullabilityOverrideChecker : DeclarationChecker {
    private val typePreparatorUnwrappingEnhancement: KotlinTypePreparator = object : KotlinTypePreparator() {
        override fun prepareType(type: KotlinTypeMarker): UnwrappedType =
            super.prepareType(type).let { it.getEnhancementDeeply() ?: it }.unwrap()
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is CallableMemberDescriptor) return
        if (descriptor.overriddenDescriptors.isEmpty()) return

        val modifierList = declaration.modifierList
        val hasOverrideNode = modifierList != null && modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        if (!hasOverrideNode) return

        val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return

        for (overriddenDescriptor in descriptor.overriddenDescriptors) {
            if (overriddenDescriptor !is JavaMethodDescriptor) continue

            val relatedTypeParameters = mutableSetOf<TypeParameterDescriptor>()
            val overridingUtilWithEnhancementUnwrapped =
                OverridingUtil
                    .createWithTypePreparatorAndCustomSubtype(typePreparatorUnwrappingEnhancement) { subtype, supertype ->
                        !JavaNullabilityChecker.isNullableTypeAgainstNotNullTypeParameter(subtype, supertype).also {
                            if (it) {
                                relatedTypeParameters.addIfNotNull(subtype.constructor.declarationDescriptor as? TypeParameterDescriptor)
                            }
                        }
                    }

            // Skip, if even with enhancement unwrapped, it's still a valid override
            if (overridingUtilWithEnhancementUnwrapped
                    .isOverridableBy(
                        overriddenDescriptor, descriptor, containingClass, true
                    ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) continue

            // Skip if it wasn't an override already before enhancement unwrappement, since errors already have been reported
            if (OverridingUtil.DEFAULT
                    .isOverridableBy(
                        overriddenDescriptor, descriptor, containingClass, true
                    ).result != OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE) continue


            val unwrappedOverridden = overriddenDescriptor.substitute(TypeSubstitutor.create(object : TypeSubstitution() {
                override fun get(key: KotlinType): TypeProjection? = null
                override fun prepareTopLevelType(topLevelType: KotlinType, position: Variance) =
                    topLevelType.getEnhancementDeeply() ?: topLevelType
            })) ?: overriddenDescriptor

            if (relatedTypeParameters.isNotEmpty()) {
                context.trace.report(
                    ErrorsJvm.WRONG_TYPE_PARAMETER_NULLABILITY_FOR_JAVA_OVERRIDE.on(declaration, relatedTypeParameters.first())
                )
            } else {
                context.trace.report(ErrorsJvm.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE.on(declaration, descriptor, unwrappedOverridden))
            }

            break
        }

    }
}
