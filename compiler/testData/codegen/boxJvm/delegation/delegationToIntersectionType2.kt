// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// DUMP_IR

fun <T> select(a: T, b: T) : T = a

interface A {
    fun foo(): Any
}
interface B {
    fun foo(): String
}
class C : A, B {
    override fun foo() = "OK"
}
class D : A, B {
    override fun foo() = "FAIL"
}

fun test(c: C, d: D): String {
    val intersection = select(c, d)
    return object: A by intersection {}.foo().toString()
}

fun box() = test(C(), D())
