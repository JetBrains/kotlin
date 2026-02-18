// TARGET_BACKEND: JS_IR, JS_IR_ES6
// MODULE: lib
// FILE: lib.kt

fun String.call() = this + "O"

inline fun O() = "".call()

object A {
    val foo = Foo
}

object Foo {
    @JsName("call")
    fun call(a: A, k: String) = k
}

inline fun K(a: A) = a.foo.call(a, "K")

// MODULE: main(lib)
// FILE: main.kt

val a = A

// CHECK_BREAKS_COUNT: function=box count=0
// CHECK_LABELS_COUNT: function=box name=$l$block count=0
fun box() = O() + K(a)
