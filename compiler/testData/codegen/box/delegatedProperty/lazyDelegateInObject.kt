// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-76628
// DUMP_IR
// DUMP_IR_AFTER_INLINE

fun lazyDelegateInObject() = object {
    val original: Any? by lazy { null }

    override fun toString(): String = if (original == null) "OK" else "FAIL"
}

fun box(): String {
    return lazyDelegateInObject().toString()
}