// TARGET_BACKEND: JVM
// WITH_REFLECT
// IGNORE_BACKEND_K1: JVM, JVM_IR

@Retention(AnnotationRetention.RUNTIME)
annotation class A(vararg val xs: String)

@A(*arrayOf("a"), *arrayOf("b"))
fun test() {}

fun box(): String {
    val annotation = ::test.annotations.single() as A
    if (!annotation.xs.contentEquals(arrayOf("a", "b"))) return annotation.toString()
    return "OK"
}

fun main() {
    println(box())
}