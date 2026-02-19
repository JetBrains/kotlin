// WITH_STDLIB
// DUMP_IR
// DUMP_IR_AFTER_INLINE

fun lazyDelegateInObject() = object {
    val original: Any? by lazy { null }

    override fun toString(): String = if (original == null) "OK" else "FAIL"
}

fun box(): String {
    return lazyDelegateInObject().toString()
}
