// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class A<T> {
    fun foo() {}
    val bar = 42
}

val test1 = A<String>::foo
val test2 = A<String>::bar
