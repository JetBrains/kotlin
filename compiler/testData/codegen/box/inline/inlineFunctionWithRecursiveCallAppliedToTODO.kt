// ISSUE: KT-81713
// IGNORE_BACKEND: NATIVE, JS_IR, WASM, JVM_IR
// Remove this comment when KT-81713 is fixed

class A<T: Int, P: Any> {
    inline fun foo(): Int = (TODO() as A<Int, T>).foo()
}

fun box(): String = "OK"