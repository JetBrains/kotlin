// IGNORE_BACKEND: JS_IR
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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: MINOR: JS_NAME_CLASH
