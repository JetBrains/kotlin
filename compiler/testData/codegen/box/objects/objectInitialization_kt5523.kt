// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
object A {
    val a = "OK"
    val b = A.a
}

fun box() = A.b
