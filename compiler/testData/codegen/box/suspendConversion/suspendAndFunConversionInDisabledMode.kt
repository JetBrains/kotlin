// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR

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
