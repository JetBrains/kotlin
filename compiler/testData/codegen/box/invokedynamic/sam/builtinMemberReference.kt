// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IntFun {
    fun invoke(i: Int): Int
}

fun invoke1(intFun: IntFun) = intFun.invoke(1)

fun box(): String {
    val test = invoke1(41::plus)
    if (test != 42) return "Failed: $test"

    return "OK"
}