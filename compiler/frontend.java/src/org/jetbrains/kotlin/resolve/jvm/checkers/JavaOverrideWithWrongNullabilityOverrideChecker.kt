/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.*

object JavaOverrideWithWrongNullabilityOverrideChecker : DeclarationChecker {
    private val overridingUtilWithEnhancementUnwrapped =
        OverridingUtil
            .createWithTypePreparatorAndCustomSubtype(JavaNullabilityChecker.typePreparatorUnwrappingEnhancement) { subtype, supertype ->
            !JavaNullabilityChecker.isNullableTypeAgainstNotNullTypeParameter(subtype, supertype)
        }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is CallableMemberDescriptor) return
        if (descriptor.overriddenDescriptors.isEmpty()) return

        val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return

        for (overriddenDescriptor in descriptor.overriddenDescriptors) {
            if (overriddenDescriptor !is JavaMethodDescriptor) continue
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

            context.trace.report(ErrorsJvm.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE.on(declaration, descriptor, unwrappedOverridden))

            break
        }

    }
}
