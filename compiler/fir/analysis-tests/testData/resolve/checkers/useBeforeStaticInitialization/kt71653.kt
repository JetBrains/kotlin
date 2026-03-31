// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// WITH_COROUTINES

object Table1 {
    init {
        runBlocking { delay(100) }
    }
    val reference = Table2
}

object Table2 {
    init {
        runBlocking { delay(100) }
    }
    val reference = Table1
}