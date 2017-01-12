// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun foo(x: Any) = x !is Runnable

fun box(): String {
	val r = object : Runnable {
		override fun run() {}
	}
	return if (!foo(r) && foo(42)) "OK" else "Fail"
}
