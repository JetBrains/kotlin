// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-32452

interface A {
    suspend fun foo(input: String): String
}

open class B<T : Any> {
    fun <U, R : Any, T> call(function: suspend T.(U) -> R): R = TODO()

    fun <U, R : Any, T> call(function: suspend T.(U) -> List<R>): List<R> = TODO()
}

class MyService : A, B<A>() {
    override suspend fun foo(input: String) = call(A::foo)
}