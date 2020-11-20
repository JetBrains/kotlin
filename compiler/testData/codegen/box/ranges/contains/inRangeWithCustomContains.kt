// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: KOTLIN_TEST_LIB
// WITH_RUNTIME
import kotlin.test.*

class Value(val x: Int) : Comparable<Value> {
    override fun compareTo(other: Value): Int {
        throw AssertionError("Should not be called")
    }
}

class ValueRange(override val start: Value,
                 override val endInclusive: Value) : ClosedRange<Value> {

    override fun contains(value: Value): Boolean {
        return value.x == 42
    }
}

operator fun Value.rangeTo(other: Value): ClosedRange<Value> = ValueRange(this, other)

fun box(): String {
    assertTrue(Value(42) in Value(1)..Value(2))
    assertTrue(Value(41) !in Value(40)..Value(42))

    return "OK"
}
