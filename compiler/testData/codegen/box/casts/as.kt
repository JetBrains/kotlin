// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

fun foo(x: Any) = x as Runnable

fun box(): String {
	val r = object : Runnable {
		override fun run() {}
	}
	return if (foo(r) === r) "OK" else "Fail"
}
