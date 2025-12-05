// RUN_PIPELINE_TILL: BACKEND

fun <T> foo(x: suspend () -> T): T = TODO()
fun foo(x: Runnable) {}

fun interface MyRunnable {
    fun run()
}

fun <T> bar(x: suspend () -> T): T = TODO()
fun bar(x: MyRunnable) {}

suspend fun mySuspend() {}

fun main() {
    foo {
        mySuspend()
        ""
    }

    foo {
        mySuspend() // Unit
    }

    bar {
        mySuspend()
        ""
    }

    bar {
        mySuspend() // Unit
    }
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, samConversion, stringLiteral, suspend, typeParameter */
