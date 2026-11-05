// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KParameter
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.TimeSource

fun box(): String {
    for (klass in listOf(
        UInt::class, ULong::class, UByte::class, UShort::class, Result::class, Duration::class, TimeSource.Monotonic.ValueTimeMark::class,
    )) {
        val members = klass.members
        for (name in listOf("hashCode", "toString")) {
            val member = members.first { it.name == name && it.parameters.singleOrNull()?.kind == KParameter.Kind.INSTANCE }
            assertEquals(listOf(KParameter.Kind.INSTANCE), member.parameters.map { it.kind }, "$klass.$name")
        }

        val equals = members.first {
            it.name == "equals" &&
                it.parameters.map { parameter -> parameter.kind } == listOf(KParameter.Kind.INSTANCE, KParameter.Kind.VALUE)
        }
        assertEquals(listOf(KParameter.Kind.INSTANCE, KParameter.Kind.VALUE), equals.parameters.map { it.kind }, "$klass.equals")
    }

    return "OK"
}
