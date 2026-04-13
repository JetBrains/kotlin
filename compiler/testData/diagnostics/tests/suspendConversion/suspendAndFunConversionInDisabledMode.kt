// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface SuspendRunnable {
    suspend fun run()
}

object Test1 {
    fun call(r: () -> Unit) {}

    object Scope {
        fun call(r: SuspendRunnable) {}

        fun bar(f: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test1.Scope.call; typeCall: function")!>call(f)<!>
        }
    }
}

object Test2 {
    fun call(r: Runnable) {}

    object Scope {
        fun call(r: SuspendRunnable) {}

        fun bar(f: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test2.Scope.call; typeCall: function")!>call(f)<!>
        }
    }
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, interfaceDeclaration, nestedClass,
objectDeclaration, samConversion, suspend */
