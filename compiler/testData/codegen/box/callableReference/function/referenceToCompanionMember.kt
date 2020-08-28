// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS

class A {
    companion object {
        fun foo(): String = "OK"
    }
}

class B {
    companion object {
        fun foo(): String = "Fail"
    }
}

fun B.foo(): String = "OK"

fun call(f: Any): String = if (f is Function0<*>) f.invoke() as String else (f as Function1<B, String>).invoke(B())

fun box(): String {
    val call1 = call(A::foo)
    if (call1 != "OK") return "fail 1: $call1"

    // Checking compatibility mode: should be resolved to extensions in 1.4
    val call2 = call(B::foo)
    if (call2 != "OK") return "fail 2: $call2"

    return "OK"
}


