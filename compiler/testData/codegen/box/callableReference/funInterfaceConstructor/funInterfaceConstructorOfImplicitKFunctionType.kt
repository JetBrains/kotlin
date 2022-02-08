// !LANGUAGE: +KotlinFunInterfaceConstructorReference

// IGNORE_BACKEND: JVM
//  ^ unsupported in old JVM BE

fun interface KRunnable {
    fun run()
}

val kr = ::KRunnable // : KFunction1<() -> Unit, KRunnable>

fun box(): String {
    var test = "Failed"
    kr { test = "OK" }.run()
    return test
}
