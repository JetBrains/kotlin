// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNCHECKED_CAST -UNUSED_LAMBDA_EXPRESSION

class Foo {
    suspend operator fun <T> invoke(body: suspend (Int) -> T) = null as T
    suspend fun <T> bar(body: suspend (Int) -> T) = null as T
}

fun <T> runBlocking(block: suspend () -> T) = null as T

public inline fun <T, R> T.run(block: T.() -> R) = null as R

fun main() {
    val retry = Foo()

    runBlocking {
        retry { 1 } // OK only after fix
    }
    runBlocking {
        retry { 1 } // OK
        1
    }
    runBlocking {
        retry.bar { 1 } // OK
    }
    runBlocking {
        { <!NON_LOCAL_SUSPENSION_POINT!>retry<!> { 1 } } // should be error
    }
    runBlocking {
        10.run { retry { 1 } } // should be OK
    }
    runBlocking {
        10.run { retry { 1 } } // should be OK
        1
    }
    runBlocking {
        { <!NON_LOCAL_SUSPENSION_POINT!>retry<!> { 1 } } // should be error
        1
    }
}
