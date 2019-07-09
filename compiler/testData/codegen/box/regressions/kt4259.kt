// TARGET_BACKEND: JVM

// WITH_RUNTIME

fun box(): String {
    val s: String? = "a"
    val o: Char? = s?.get(0)
    val c: Any? = o?.javaClass
    return if (c !=  null) "OK"  else "fail"
}
