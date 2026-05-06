// TARGET_BACKEND: JVM
// WITH_REFLECT
// OPT_IN: kotlin.concurrent.atomics.ExperimentalAtomicApi
// FULL_JDK

import kotlin.concurrent.atomics.AtomicInt
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(emptyList(), Cloneable::class.constructors)
    assertEquals(emptyList(), Function1::class.constructors)

    assertEquals(
        "fun `<init>`(): kotlin.Any",
        Any::class.constructors.joinToString("\n"),
    )
    assertEquals(
        "fun `<init>`(): kotlin.String",
        String::class.constructors.joinToString("\n"),
    )
    assertEquals(
        "fun `<init>`(): kotlin.Number",
        Number::class.constructors.joinToString("\n"),
    )
    assertEquals(
        "fun `<init>`(kotlin.String, kotlin.Int): kotlin.Enum<E>",
        Enum::class.constructors.joinToString("\n"),
    )

    assertEquals(
        "fun `<init>`(): kotlin.Float",
        Float::class.constructors.joinToString("\n"),
    )
    assertEquals(
        "fun `<init>`(): kotlin.Float",
        Float::class.javaObjectType.kotlin.constructors.joinToString("\n"),
    )

    assertEquals(
        """
        fun `<init>`(): kotlin.Throwable
        fun `<init>`(kotlin.String!, kotlin.Throwable!, kotlin.Boolean, kotlin.Boolean): kotlin.Throwable
        fun `<init>`(kotlin.String?): kotlin.Throwable
        fun `<init>`(kotlin.String?, kotlin.Throwable?): kotlin.Throwable
        fun `<init>`(kotlin.Throwable?): kotlin.Throwable
        """.trimIndent(),
        Throwable::class.constructors.sortedBy(Any::toString).joinToString("\n"),
    )

    // Currently these use unmapped Java atomic types, but this might change in KT-75220.
    assertEquals(
        """
        fun `<init>`(): java.util.concurrent.atomic.AtomicInteger
        fun `<init>`(kotlin.Int): java.util.concurrent.atomic.AtomicInteger
        """.trimIndent(),
        AtomicInt::class.constructors.sortedBy(Any::toString).joinToString("\n"),
    )

    return "OK"
}
