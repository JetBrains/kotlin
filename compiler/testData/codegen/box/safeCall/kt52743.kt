// JVM_ABI_K1_K2_DIFF: KT-63855

fun <T: Any?> nullableFun(): T {
    return null as T
}

fun box(): String {
    val t = nullableFun<String>()
    return if (t?.length == null) "OK" else "Fail"
}
