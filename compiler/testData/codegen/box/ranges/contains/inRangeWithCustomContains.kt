// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

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
    assert(Value(42) in Value(1)..Value(2))
    assert(Value(41) !in Value(40)..Value(42))

    return "OK"
}
