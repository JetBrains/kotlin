// WITH_STDLIB
fun box(): String {
    val x by lazy { "OK" }
    return x
}