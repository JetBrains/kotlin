// IGNORE_BACKEND: ANDROID

annotation class A(val i: Int)
annotation class B(val i: Int)

fun box(): String {
    val a = A(1)
    val b = B(1)
    if (a == b) return "Fail"
    return "OK"
}
