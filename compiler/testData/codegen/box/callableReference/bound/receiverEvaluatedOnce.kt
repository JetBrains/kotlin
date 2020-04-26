var x = 0

class A {
    fun f() = if (x == 1) "OK" else "Fail $x"
}

fun callTwice(f: () -> String): String {
    f()
    return f()
}

fun box(): String {
    return callTwice(({ x++; A() }())::f)
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: BINDING_RECEIVERS
