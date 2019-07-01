// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

class A {
    fun foo(x: Int, y: Int) = x + y
}

fun box(): String {
    val x = (A::foo).call(A(), 42, 239)
    if (x != 281) return "Fail: $x"

    try {
        (A::foo).call()
        return "Fail: no exception"
    }
    catch (e: Exception) {}

    return "OK"
}
