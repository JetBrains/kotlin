/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * KLIBs store declaration origins by name, so [IrDeclarationDeserializer] keeps resolving renamed origins under
 * their legacy names via [IrDeclarationDeserializer.legacyDeclarationOriginNames]. A legacy name must never be
 * reused for a current origin, otherwise the alias would silently shadow it (KT-90675).
 *
 * For the same reason, the set of [IrDeclarationOrigin.Companion] members is pinned by [expectedDeclarationOriginNames]:
 * a KLIB written with an origin name unknown to the reading compiler loses that origin, so adding, removing or renaming
 * a member affects KLIB compatibility and must be agreed with the Kotlin Common Backend team.
 */
class IrDeclarationOriginDeserializationTest {
    /**
     * Names of all [IrDeclarationOrigin.Companion] members, in declaration order.
     * Update this list only after consulting the Kotlin Common Backend team.
     */
    private val expectedDeclarationOriginNames: Set<String> = setOf(
        "DEFINED",
        "FAKE_OVERRIDE",
        "FOR_LOOP_ITERATOR",
        "FOR_LOOP_VARIABLE",
        "FOR_LOOP_IMPLICIT_VARIABLE",
        "PROPERTY_BACKING_FIELD",
        "DEFAULT_PROPERTY_ACCESSOR",
        "DELEGATE",
        "PROPERTY_DELEGATE",
        "DELEGATED_PROPERTY_ACCESSOR",
        "DELEGATED_MEMBER",
        "ENUM_CLASS_SPECIAL_MEMBER",
        "FUNCTION_FOR_DEFAULT_PARAMETER",
        "MASK_FOR_DEFAULT_FUNCTION",
        "DEFAULT_CONSTRUCTOR_MARKER",
        "SYNTHETIC_CONSTRUCTOR_MARKER",
        "METHOD_HANDLER_IN_DEFAULT_FUNCTION",
        "MOVED_DISPATCH_RECEIVER",
        "MOVED_EXTENSION_RECEIVER",
        "MOVED_CONTEXT_RECEIVER",

        "FILE_CLASS",
        "SYNTHETIC_FILE_CLASS",
        "JVM_MULTIFILE_CLASS",
        "ERROR_CLASS",

        "SCRIPT_CLASS",
        "SCRIPT_THIS_RECEIVER",
        "SCRIPT_STATEMENT",
        "SCRIPT_EARLIER_SCRIPTS",
        "SCRIPT_CALL_PARAMETER",
        "SCRIPT_IMPLICIT_RECEIVER",
        "SCRIPT_PROVIDED_PROPERTY",
        "SCRIPT_RESULT_PROPERTY",
        "REPL_EVAL_FUNCTION",
        "REPL_FROM_OTHER_SNIPPET",
        "GENERATED_DATA_CLASS_MEMBER",
        "GENERATED_INLINE_CLASS_MEMBER",
        "GENERATED_FULL_VALUE_CLASS_MEMBER",
        "LOCAL_FUNCTION",
        "LOCAL_FUNCTION_FOR_LAMBDA",
        "CATCH_PARAMETER",
        "UNDERSCORE_PARAMETER",
        "DESTRUCTURED_OBJECT_PARAMETER",
        "INSTANCE_RECEIVER",
        "PRIMARY_CONSTRUCTOR_PARAMETER",
        "IR_DESTRUCTURED_PARAMETER_VARIABLE",
        "IR_TEMPORARY_VARIABLE",
        "IR_TEMPORARY_VARIABLE_FOR_INLINED_PARAMETER",
        "IR_TEMPORARY_VARIABLE_FOR_INLINED_EXTENSION_RECEIVER",
        "IR_EXTERNAL_DECLARATION_STUB",
        "IR_EXTERNAL_JAVA_DECLARATION_STUB",
        "IR_BUILTINS_STUB",
        "BRIDGE",
        "BRIDGE_SPECIAL",
        "GENERATED_SETTER_GETTER",

        "FIELD_FOR_ENUM_ENTRY",
        "SYNTHETIC_HELPER_FOR_ENUM_VALUES",
        "SYNTHETIC_HELPER_FOR_ENUM_ENTRIES",
        "FIELD_FOR_ENUM_VALUES",
        "FIELD_FOR_ENUM_ENTRIES",
        "PROPERTY_FOR_ENUM_ENTRIES",
        "FIELD_FOR_OBJECT_INSTANCE",
        "FIELD_FOR_CLASS_CONTEXT_RECEIVER",

        "ADAPTER_FOR_CALLABLE_REFERENCE",
        "ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE",
        "ADAPTER_FOR_SUSPEND_CONVERSION",
        "ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION",
        "ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR",

        "GENERATED_SAM_IMPLEMENTATION",
        "SYNTHETIC_GENERATED_SAM_IMPLEMENTATION",

        "SYNTHETIC_JAVA_PROPERTY_DELEGATE",

        "FIELD_FOR_OUTER_THIS",
        "CONTINUATION",
        "LOWERED_SUSPEND_FUNCTION",

        "SHARED_VARIABLE_IN_EVALUATOR_FRAGMENT",

        "SYNTHETIC_ACCESSOR",
        "SYNTHETIC_ACCESSOR_CAPTURED_TYPE_PARAMETER",
        "FILLED_FOR_UNBOUND_SYMBOL",
        "INLINE_LAMBDA",
        "FUNCTION_INTERFACE_MEMBER",
        "STUB_FOR_LENIENT",
        "STUB_FOR_TYPE_SWITCH",
        "VERSION_OVERLOAD_WRAPPER",
        "LAMBDA_EXTENSION_RECEIVER",
    )

    @Test
    fun testNoNewDeclarationOrigins() {
        val added = IrDeclarationDeserializer.currentDeclarationOriginNames.keys - expectedDeclarationOriginNames
        assertTrue(added.isEmpty()) {
            "New IrDeclarationOrigin members $added might break Klib forward compatibility. Consult Kotlin Common Backend team"
        }
    }

    @Test
    fun testNoRemovedDeclarationOrigins() {
        val removed = expectedDeclarationOriginNames - IrDeclarationDeserializer.currentDeclarationOriginNames.keys
        assertTrue(removed.isEmpty()) {
            "Removed IrDeclarationOrigin members $removed might break Klib backward compatibility. Consult Kotlin Common Backend team"
        }
    }

    @Test
    fun testLegacyOriginNamesDoNotCollideWithCurrentOnes() {
        val collisions = IrDeclarationDeserializer.legacyDeclarationOriginNames.keys
            .filter { it in IrDeclarationDeserializer.currentDeclarationOriginNames }
        assertTrue(collisions.isEmpty()) {
            "Legacy origin names must not collide with current ones: $collisions"
        }
    }

    @Test
    fun testLegacyOriginNamesResolveToCurrentOrigins() {
        for ([legacyName, origin] in IrDeclarationDeserializer.legacyDeclarationOriginNames) {
            assertTrue(origin.name in IrDeclarationDeserializer.currentDeclarationOriginNames) {
                "Legacy name $legacyName maps to $origin, which is not a current origin"
            }
        }
    }
}
