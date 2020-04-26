// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE
// DONT_TARGET_EXACT_BACKEND: WASM

fun foo(x: Any) = x as Runnable

fun box(): String {
	val r = object : Runnable {
		override fun run() {}
	}
	return if (foo(r) === r) "OK" else "Fail"
}
