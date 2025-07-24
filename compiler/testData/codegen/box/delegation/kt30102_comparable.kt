// JVM_TARGET: 1.8
// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)

fun box(): String {
    val a = BooleanWrap(false)
    return if (a < true) "OK" else "Fail"
}

class BooleanWrap(private val value: Boolean): Comparable<Boolean> by value
