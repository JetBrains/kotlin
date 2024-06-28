// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

class Foo(val s: String)
fun foo(): Foo? = Foo("OK")

fun <T> run(f: () -> T): T = f()

val foo: Foo = run {
    val x = foo()
    if (x == null) throw Exception()
    x
}

fun box() = foo.s
