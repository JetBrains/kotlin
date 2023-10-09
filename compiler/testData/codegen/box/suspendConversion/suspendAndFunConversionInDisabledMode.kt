// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// IGNORE_BACKEND: JVM
// JVM_ABI_K1_K2_DIFF: KT-62904

fun interface Runnable {
    fun run()
}

fun interface SuspendRunnable {
    suspend fun run()
}

object Test1 {
    fun call(r: () -> Unit) {}

    object Scope {
        fun call(r: SuspendRunnable) {}

        fun bar(f: () -> Unit) {
            call(f)
        }
    }
}

object Test2 {
    fun call(r: Runnable) {}

    object Scope {
        fun call(r: SuspendRunnable) {}

        fun bar(f: () -> Unit) {
            call(f)
        }
    }
}

fun box(): String {
    Test1.Scope.bar {  }
    Test2.Scope.bar {  }
    return "OK"
}
