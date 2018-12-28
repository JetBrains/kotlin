// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

const val MaxUI = UInt.MAX_VALUE
const val MinUI = UInt.MIN_VALUE

const val MaxUL = ULong.MAX_VALUE
const val MinUL = ULong.MIN_VALUE

val M1 = MaxUI.toULong()
val M2 = M1 + 10UL

val u_1_10 = 1u .. 10u
val ul_1_10 = 1UL..10UL
val minUI_maxUI = MinUI..MaxUI
val minUL_maxUL = MinUL..MaxUL
val m1_m2 = M1..M2

fun box(): String {
    if (0u in u_1_10) throw AssertionError()
    if (1u !in u_1_10) throw AssertionError()
    if (5u !in u_1_10) throw AssertionError()
    if (10u !in u_1_10) throw AssertionError()
    if (20u in u_1_10) throw AssertionError()

    if (0UL in ul_1_10) throw AssertionError()
    if (1UL !in ul_1_10) throw AssertionError()
    if (5UL !in ul_1_10) throw AssertionError()
    if (10UL !in ul_1_10) throw AssertionError()
    if (20UL in ul_1_10) throw AssertionError()

    if (0u !in minUI_maxUI) throw AssertionError()
    if (MinUI !in minUI_maxUI) throw AssertionError()
    if (MaxUI !in minUI_maxUI) throw AssertionError()

    if (0UL !in minUL_maxUL) throw AssertionError()
    if (MinUL !in minUL_maxUL) throw AssertionError()
    if (MaxUL !in minUL_maxUL) throw AssertionError()

    if (0UL in m1_m2) throw AssertionError()
    if (1UL in m1_m2) throw AssertionError()
    if (10UL in m1_m2) throw AssertionError()
    if (M1 !in m1_m2) throw AssertionError()
    if (M1+1UL !in m1_m2) throw AssertionError()
    if (M2 !in m1_m2) throw AssertionError()

    return "OK"
}