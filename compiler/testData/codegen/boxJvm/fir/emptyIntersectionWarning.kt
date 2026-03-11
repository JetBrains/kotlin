// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

interface I
class View1

fun <T : View1> findViewById1(): T? = null
fun test1(): I? = findViewById1()

fun box(): String {
    test1()
    return "OK"
}
