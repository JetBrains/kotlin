// WITH_STDLIB
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class A(val x: String)

fun foo(m: Map<String, Int>) {
    @A("foo/test")
    val test by lazy { 42 }
}
