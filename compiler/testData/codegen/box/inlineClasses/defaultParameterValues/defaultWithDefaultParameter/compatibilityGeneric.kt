// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// !JVM_DEFAULT_MODE: compatibility
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8

interface Path {
    fun dispatch(s: String = "K") = "dispatch$s"
    fun Int.extension(s: String = "K") = "${this}extension$s"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class RealPath<T: Int>(val x: T) : Path

fun box(): String {
    val rp = RealPath(1)
    val res = "${rp.dispatch()};${rp.dispatch("KK")};" +
            with(rp) {
                "${1.extension()};${2.extension("KK")}"
            }
    if (res != "dispatchK;dispatchKK;1extensionK;2extensionKK") return res
    return "OK"
}
