// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

class A(val result: String = "OK") {
    fun foo(x: Int = 42): String {
        assert(x == 42) { x }
        return result
    }
}

fun box(): String = A::foo.callBy(mapOf(A::foo.parameters.first() to A()))
