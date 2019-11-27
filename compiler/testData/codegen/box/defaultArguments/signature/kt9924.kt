// IGNORE_BACKEND_FIR: JVM_IR
abstract class A<T> {
    abstract fun test(a: T, b:Boolean = false) : String
}

class B : A<String>() {
    override fun test(a: String, b: Boolean): String {
        return a
    }
}

fun box(): String {
    return B().test("OK")
}