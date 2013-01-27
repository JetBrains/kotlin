fun foo(x: Any) = x as Runnable

fun box(): String {
	val r = object : Runnable {
		override fun run() {}
	}
	return if (foo(r) identityEquals r) "OK" else "Fail"
}
