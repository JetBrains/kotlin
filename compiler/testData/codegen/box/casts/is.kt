// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

fun foo(x: Any) = x is Runnable

fun box(): String {
	val r = object : Runnable {
		override fun run() {}
	}
	return if (foo(r) && !foo(42)) "OK" else "Fail"
}
