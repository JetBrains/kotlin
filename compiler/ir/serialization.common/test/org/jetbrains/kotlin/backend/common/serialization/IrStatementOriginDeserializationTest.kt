/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * KLIBs store statement origins by name, so [IrBodyDeserializer] keeps resolving renamed origins under
 * their legacy names via [IrBodyDeserializer.legacyStatementOriginNames]. A legacy name must never be
 * reused for a current origin, otherwise the current origin would silently replace the alias (KT-91139).
 * The legacy names themselves are pinned by [expectedLegacyStatementOriginNames]: dropping one makes the
 * compiler misread every KLIB which carries it.
 *
 * For the same reason, the set of [IrStatementOrigin.Companion] members is pinned by [expectedStatementOriginNames]:
 * a KLIB written with an origin name unknown to the reading compiler loses that origin, so adding, removing or renaming
 * a member affects KLIB compatibility and must be agreed with the Kotlin Common Backend team.
 */
class IrStatementOriginDeserializationTest {
    /**
     * Debug names of all [IrStatementOrigin.Companion] members, in declaration order.
     * Update this list only after consulting the Kotlin Common Backend team. When a member is renamed, do not just
     * replace its name here: older KLIBs still carry the old name, which must be aliased and pinned, see
     * [expectedLegacyStatementOriginNames].
     */
    private val expectedStatementOriginNames: Set<String> = setOf(
        "SAFE_CALL",
        "ERROR_SAFE_CALL",

        "UMINUS",
        "UPLUS",
        "EXCL",
        "EXCLEXCL",

        "ELVIS",

        "LT",
        "GT",
        "LTEQ",
        "GTEQ",

        "EQEQ",
        "EQEQEQ",
        "EXCLEQ",
        "EXCLEQEQ",

        "IN",
        "NOT_IN",
        "ANDAND",
        "OROR",

        "PLUS",
        "MINUS",
        "MUL",
        "DIV",
        "PERC",
        "RANGE",
        "RANGE_UNTIL",

        "INVOKE",
        "VARIABLE_AS_FUNCTION",
        "GET_ARRAY_ELEMENT",

        "PREFIX_INCR",
        "PREFIX_DECR",
        "POSTFIX_INCR",
        "POSTFIX_DECR",

        "EQ",
        "PLUSEQ",
        "MINUSEQ",
        "MULTEQ",
        "DIVEQ",
        "PERCEQ",

        "ARGUMENTS_REORDERING_FOR_CALL",
        "IMPLICIT_ARGUMENT",
        "DESTRUCTURING_DECLARATION",

        "GET_PROPERTY",
        "GET_LOCAL_PROPERTY",

        "IF",
        "WHEN",
        "WHEN_COMMA",
        "WHILE_LOOP",
        "DO_WHILE_LOOP",
        "FOR_LOOP",
        "FOR_LOOP_ITERATOR",
        "FOR_LOOP_INNER_WHILE",
        "FOR_LOOP_HAS_NEXT",
        "FOR_LOOP_NEXT",

        "LAMBDA",
        "DEFAULT_VALUE",
        "ANONYMOUS_FUNCTION",
        "OBJECT_LITERAL",
        "COLLECTION_LITERAL",
        "ADAPTED_FUNCTION_REFERENCE",

        "FUNCTION_TYPE_EXPRESSION_CONVERSION",
        "FUN_INTERFACE_CONSTRUCTOR_REFERENCE",

        "INITIALIZE_PROPERTY_FROM_PARAMETER",
        "INITIALIZE_FIELD",

        "PROPERTY_REFERENCE_FOR_DELEGATE",

        "BRIDGE_DELEGATION",

        "SYNTHETIC_NOT_AUTOBOXED_CHECK",

        "PARTIAL_LINKAGE_RUNTIME_ERROR",

        "SYNTHESIZED_INIT_BLOCK",
        "DEFAULT_DISPATCH_CALL",
        "INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE",
        "INLINED_FUNCTION_REFERENCE",
        "INLINE_LAMBDA",
        "INLINE_ARGS_CONTAINER",
    )

    /**
     * Legacy origin names which must keep resolving, with the debug names of the origins they resolve to. It must
     * match [IrBodyDeserializer.legacyStatementOriginNames]: extend both when a member is renamed.
     */
    private val expectedLegacyStatementOriginNames: Map<String, String> = mapOf(
        "SUSPEND_CONVERSION" to "FUNCTION_TYPE_EXPRESSION_CONVERSION",
    )

    @Test
    fun testNoNewStatementOrigins() {
        val added = IrBodyDeserializer.currentStatementOriginNames.keys - expectedStatementOriginNames
        assertTrue(added.isEmpty()) {
            "New IrStatementOrigin members $added might break KLIB forward compatibility. Consult the Kotlin Common Backend " +
                    "team, then add them to expectedStatementOriginNames"
        }
    }

    @Test
    fun testNoRemovedStatementOrigins() {
        val removed = expectedStatementOriginNames - IrBodyDeserializer.currentStatementOriginNames.keys
        assertTrue(removed.isEmpty()) {
            "Removed IrStatementOrigin members $removed might break KLIB backward compatibility. Consult the Kotlin Common " +
                    "Backend team. If a member was renamed, older KLIBs still carry its old name: alias it in " +
                    "IrBodyDeserializer.legacyStatementOriginNames and pin the alias in expectedLegacyStatementOriginNames. " +
                    "The alias does not help the compilers released earlier, which do not know the new name: prefer not to rename"
        }
    }

    @Test
    fun testLegacyOriginNamesDoNotCollideWithCurrentOnes() {
        val collisions = IrBodyDeserializer.legacyStatementOriginNames.keys
            .filter { it in IrBodyDeserializer.currentStatementOriginNames }
        assertTrue(collisions.isEmpty()) {
            "Legacy origin names must not collide with current ones: $collisions"
        }
    }

    @Test
    fun testLegacyOriginNamesResolveToCurrentOrigins() {
        for ([legacyName, origin] in IrBodyDeserializer.legacyStatementOriginNames) {
            // By identity: an origin is equal to a member whenever it has the member's debug name, so an equality check
            // would only tell that the alias points to a current name.
            assertSame(IrBodyDeserializer.currentStatementOriginNames[origin.debugName], origin) {
                "Legacy name $legacyName maps to $origin, which is not a member of IrStatementOrigin.Companion"
            }
        }
    }

    @Test
    fun testCurrentOriginNamesAreDeserialized() {
        for ([name, origin] in IrBodyDeserializer.currentStatementOriginNames) {
            assertSame(origin, IrBodyDeserializer.resolveStatementOrigin(name)) {
                "Origin name $name must be deserialized as the member of IrStatementOrigin.Companion which carries it. " +
                        "Make sure IrBodyDeserializer.resolveStatementOrigin looks the name up in currentStatementOriginNames " +
                        "and does not handle it earlier, as it does for names which start with COMPONENT_"
            }
        }
    }

    @Test
    fun testLegacyOriginNamesAreDeserialized() {
        val actualLegacyNames = IrBodyDeserializer.legacyStatementOriginNames.mapValues { it.value.debugName }
        assertEquals(expectedLegacyStatementOriginNames, actualLegacyNames) {
            "IrBodyDeserializer.legacyStatementOriginNames differs from the pinned legacy origin names. " +
                    "Dropping a legacy name makes the compiler misread older KLIBs; pin a new one in this test as well"
        }
        for ([legacyName, currentName] in expectedLegacyStatementOriginNames) {
            assertEquals(currentName, IrBodyDeserializer.resolveStatementOrigin(legacyName).debugName) {
                "Legacy origin name $legacyName must be deserialized as $currentName, otherwise older KLIBs are misread. " +
                        "Make sure IrBodyDeserializer.resolveStatementOrigin looks the name up in legacyStatementOriginNames"
            }
        }
    }

    @Test
    fun testComponentAndUnknownOriginNamesAreDeserialized() {
        assertEquals("COMPONENT_3", IrStatementOrigin.COMPONENT_N.withIndex(3).debugName) {
            "IrStatementOrigin.COMPONENT_N is serialized to KLIBs as COMPONENT_<index>: changing that name affects KLIB " +
                    "compatibility in the same way as renaming a member of IrStatementOrigin.Companion"
        }
        assertEquals(IrStatementOrigin.COMPONENT_N.withIndex(3), IrBodyDeserializer.resolveStatementOrigin("COMPONENT_3")) {
            "COMPONENT_<index> must be deserialized as IrStatementOrigin.COMPONENT_N with that index"
        }

        val unknownName = "ORIGIN_UNKNOWN_TO_THIS_COMPILER"
        assertEquals(unknownName, IrBodyDeserializer.resolveStatementOrigin(unknownName).debugName) {
            "An origin name unknown to this compiler must be deserialized as an origin which carries that name"
        }
    }
}
