/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.config.LanguageFeature.OverloadResolutionSpecificityForEnhancedJvmPrimitiveWrappers
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.K2Only
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext

open class JvmTypeSpecificityComparator(
    open val context: TypeSystemInferenceExtensionContext,
    private val languageVersionSettings: LanguageVersionSettings,
) : TypeSpecificityComparator {

    override fun isDefinitelyLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean {
        if (isDefinitelyLessSpecificForFlexibleSpecific(specific, general)) return true

        if (!context.isK2 || !languageVersionSettings.supportsFeature(OverloadResolutionSpecificityForEnhancedJvmPrimitiveWrappers)) return false

        @OptIn(K2Only::class)
        return isDefinitelyLessSpecificForSameEnhancedNotNullable(specific, general)
    }

    /**
     * Checks if `specific` is a flexible primitive (wrapper in Java) and `general` is a not-nullable primitive.
     * For `Int!` and `Int` returns true
     * For `Int!` and `Byte` returns true
     * For `Int!` and `Int!` returns false
     * For `@EnhancedNullability Int` (`@NotNull Integer`) and `Int` returns false
     */
    private fun isDefinitelyLessSpecificForFlexibleSpecific(
        specific: KotlinTypeMarker,
        general: KotlinTypeMarker,
    ): Boolean = with(context) {
        val simpleGeneral = general.asRigidType()
        if (!specific.isFlexible() || simpleGeneral == null) return false

        // general is inflexible
        val flexibility = specific.asFlexibleType()!!

        // For primitive types we have to take care of the case when there are two overloaded methods like
        //    foo(int) and foo(Integer)
        // if we do not discriminate one of them, any call to foo(kotlin.Int) will result in overload resolution ambiguity
        // so, for such cases, we discriminate Integer in favour of int
        if (!simpleGeneral.isPrimitiveType() || !flexibility.lowerBound().isPrimitiveType()) {
            return false
        }

        // Int? >< Int!
        if (simpleGeneral.isMarkedNullable()) return false
        // Int! lessSpecific Int
        return true
    }

    /**
     * For `Int!` and `Int` returns true
     * For `Int!` and `Byte` returns false
     * For `Int!` and `Int!` returns false
     * For `@EnhancedNullability Int` (`@NotNull Integer`) and `Int` returns true.
     * For `@EnhancedNullability Int` (`@NotNull Integer`) and `Byte` returns false.
     *
     * Potentially, we might use just this function (without requiring type constructors equality)
     * instead of [isDefinitelyLessSpecificForFlexibleSpecific], but it would be a breaking change for the call `foo(1)`
     * which currently resolves to (2)
     *     public static void foo(byte b) {} // (1)
     *     public static void foo(@NotNull Integer b) {} // (2)
     */
    @K2Only
    private fun isDefinitelyLessSpecificForSameEnhancedNotNullable(
        specific: KotlinTypeMarker,
        general: KotlinTypeMarker,
    ): Boolean = with(context) {
        // General should be `int`, `char`, ...
        if (!general.isPrimitiveInJava()) return false

        // Specific should refer to the same class.
        // Without this requirement we would assume `@NotNull Integer` less specific `byte`, which would be a breaking change
        // we're not ready for.
        if (!areEqualTypeConstructors(specific.typeConstructor(), general.typeConstructor())) return false

        // But it should not be mapped to the primitive because it's nullable/flexible/enhanced-to-not-nullable
        return !specific.isPrimitiveInJava()
    }

    @K2Only
    private fun KotlinTypeMarker.isPrimitiveInJava(): Boolean = with(context) {
        val rigid = asRigidType() ?: return false
        if (rigid.isNullableType() || rigid.hasEnhancedNullability()) return false

        return rigid.isPrimitiveType()
    }
}
