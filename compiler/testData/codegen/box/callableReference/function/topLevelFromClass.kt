// IGNORE_BACKEND_FIR: JVM_IR
fun <T> run(arg1: T, arg2: T, funRef:(T,T) -> T): T {
    return funRef(arg1, arg2)
}

fun foo(o: Int, k: Int) = o + k

class A {
    fun bar() = (::foo)(111, 222)
}

fun box(): String {
    val result = A().bar()
    if (result != 333) return "Fail $result"

    var r = run(111, 222, ::foo)
    if (result != 333) return "Fail $result"

    return "OK"
}
