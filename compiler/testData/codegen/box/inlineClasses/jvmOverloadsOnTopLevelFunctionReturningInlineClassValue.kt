// TARGET_BACKEND: JVM
// WITH_RUNTIME

inline class Str(val s: String)

@JvmOverloads
fun test(so: String = "O", sk: String = "K") = Str(so + sk)

fun box(): String =
    test().s