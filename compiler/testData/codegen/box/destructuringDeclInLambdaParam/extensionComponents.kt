// IGNORE_BACKEND_FIR: JVM_IR
class A<T>(val x: String, val y: String, val z: T)

fun <T> foo(a: A<T>, block: (A<T>) -> String): String = block(a)

operator fun A<*>.component1() = x

object B {
    operator fun A<*>.component2() = y
}

fun B.bar(): String {

    operator fun <R> A<R>.component3() = z

    val x = foo(A("O", "K", 123)) { (x, y, z) -> x + y + z.toString() }
    if (x != "OK123") return "fail 1: $x"

    return "OK"
}

fun box() = B.bar()
