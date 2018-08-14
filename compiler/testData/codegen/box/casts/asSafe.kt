// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun foo(x: Any) = x as? Runnable

fun box(): String {
	val r = object : Runnable {
		override fun run() {}
	}
	return if (foo(r) === r) "OK" else "Fail"
}
