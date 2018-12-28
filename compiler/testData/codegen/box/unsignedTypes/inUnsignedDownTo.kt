// IGNORE_BACKEND: JVM_IR, JS_IR
// WITH_RUNTIME

const val MaxUI = UInt.MAX_VALUE
const val MinUI = UInt.MIN_VALUE

val M1 = MaxUI.toULong()
val M2 = M1 + 10UL

fun box(): String {
    if (0u in 10u downTo 1u) throw AssertionError()
    if (1u !in 10u downTo 1u) throw AssertionError()
    if (5u !in 10u downTo 1u) throw AssertionError()
    if (10u !in 10u downTo 1u) throw AssertionError()
    if (20u in 10u downTo 1u) throw AssertionError()

    if (0UL in 10UL downTo 1UL) throw AssertionError()
    if (1UL !in 10UL downTo 1UL) throw AssertionError()
    if (5UL !in 10UL downTo 1UL) throw AssertionError()
    if (10UL !in 10UL downTo 1UL) throw AssertionError()
    if (20UL in 10UL downTo 1UL) throw AssertionError()

    if (0UL in M2 downTo M1) throw AssertionError()
    if (1UL in M2 downTo M1) throw AssertionError()
    if (10UL in M2 downTo M1) throw AssertionError()
    if (M1 !in M2 downTo M1) throw AssertionError()
    if (M1+1UL !in M2 downTo M1) throw AssertionError()
    if (M2 !in M2 downTo M1) throw AssertionError()

    return "OK"
}