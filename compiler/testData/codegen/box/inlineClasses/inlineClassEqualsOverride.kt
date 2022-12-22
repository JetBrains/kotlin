// WITH_STDLIB
// LANGUAGE: +ValueClasses, +CustomEqualsInValueClasses
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import kotlin.math.abs

@JvmInline
@AllowTypedEquals
value class IC1(val value: Double) {
    @TypedEquals
    fun equals(other: IC1): Boolean {
        return abs(value - other.value) < 0.1
    }
}
@OptIn(AllowTypedEquals::class)
interface I {
    fun equals(param: IC2): Boolean
}

@JvmInline
@AllowTypedEquals
value class IC2(val value: Int) : I {
    @TypedEquals
    override fun equals(param: IC2): Boolean {
        return abs(value - param.value) < 2
    }
}

@JvmInline
value class IC3(val value: Int) {

}

@JvmInline
value class IC4(val value: Int) {
    override fun equals(other: Any?) = TODO()
}

@JvmInline
@AllowTypedEquals
value class IC5(val value: Int) {
    @TypedEquals
    fun equals(other: IC5): Boolean = TODO()
}

@JvmInline
value class IC6(val value: Int) {
    override fun equals(other: Any?): Nothing = TODO()
}

@JvmInline
@AllowTypedEquals
value class IC7(val value: Int) {
    fun equals(other: IC7) = true
}

inline fun <reified T> assertThrows(block: () -> Unit): Boolean {
    try {
        block.invoke()
    } catch (t: Throwable) {
        return t is T
    }
    return false
}

@OptIn(AllowTypedEquals::class)
fun box() = when {
    IC1(1.0) != IC1(1.05) -> "Fail 1.1"
    (IC1(1.0) as Any) != IC1(1.05) -> "Fail 1.2"
    IC1(1.0) != (IC1(1.05) as Any) -> "Fail 1.3"
    (IC1(1.0) as Any) != (IC1(1.05) as Any) -> "Fail 1.4"

    IC1(1.0) == IC1(1.2) -> "Fail 2.1"
    (IC1(1.0) as Any) == IC1(1.2) -> "Fail 2.2"
    IC1(1.0) == (IC1(1.2) as Any) -> "Fail 2.3"
    (IC1(1.0) as Any) == (IC1(1.2) as Any) -> "Fail 2.4"

    IC2(5) != IC2(6) -> "Fail 3.1"
    (IC2(5) as Any) != IC2(6) -> "Fail 3.2"
    IC2(5) != (IC2(6) as Any) -> "Fail 3.3"
    (IC2(5) as Any) != (IC2(6) as Any) -> "Fail 3.4"

    IC2(5) == IC2(7) -> "Fail 4.1"
    (IC2(5) as Any) == IC2(7) -> "Fail 4.2"
    IC2(5) == (IC2(7) as Any) -> "Fail 4.3"
    (IC2(5) as Any) == (IC2(7) as Any) -> "Fail 4.4"

    IC3(5) != IC3(5) -> "Fail 5.1"
    (IC3(5) as Any) != IC3(5) -> "Fail 5.2"
    IC3(5) != (IC3(5) as Any) -> "Fail 5.3"
    (IC3(5) as Any) != (IC3(5) as Any) -> "Fail 5.4"

    IC3(5) == IC3(6) -> "Fail 6.1"
    (IC3(5) as Any) == IC3(6) -> "Fail 6.2"
    IC3(5) == (IC3(6) as Any) -> "Fail 6.3"
    (IC3(5) as Any) == (IC3(6) as Any) -> "Fail 6.4"

    IC1(1.0) == Any() -> "Fail 7.1"
    (IC1(1.0) as Any) == Any() -> "Fail 7.2"

    !assertThrows<NotImplementedError> { IC5(0) == IC5(1) } -> "Fail 8.1"
    !assertThrows<NotImplementedError> { IC6(0) == IC6(1) } -> "Fail 8.2"

    IC7(1) == IC7(2) -> "Fail 9"

    else -> "OK"
}
