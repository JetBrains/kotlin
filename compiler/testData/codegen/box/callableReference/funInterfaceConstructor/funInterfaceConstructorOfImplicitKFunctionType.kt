// !LANGUAGE: +KotlinFunInterfaceConstructorReference

// IGNORE_BACKEND: JVM
//  ^ unsupported in old JVM BE

// JVM_ABI_K1_K2_DIFF: KT-63861

fun interface KRunnable {
    fun run()
}

val kr = ::KRunnable // : KFunction1<() -> Unit, KRunnable>

fun box(): String {
    var test = "Failed"
    kr { test = "OK" }.run()
    return test
}
