// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6

class Foo(val s: String)
fun foo(): Foo? = Foo("OK")

fun <T> run(f: () -> T): T = f()

val foo: Foo = run {
    val x = foo()
    if (x == null) throw Exception()
    x
}

fun box() = foo.s
