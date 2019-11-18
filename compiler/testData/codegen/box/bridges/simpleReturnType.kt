// IGNORE_BACKEND_FIR: JVM_IR
open class A<T : Number>(val t: T) {
    open fun foo(): T = t
}

class Z : A<Int>(17) {
    override fun foo() = 239
}

fun box(): String {
    val z = Z()
    val a: A<Int> = z
    return when {
        z.foo() != 239 -> "Fail #1"
        a.foo() != 239 -> "Fail #2"
        else -> "OK"
    }
}
