fun <T: Any?> nullableFun(): T {
    return null as T
}

fun box(): String {
    val t = nullableFun<String>()
    return if (t?.length == null) "OK" else "Fail"
}
