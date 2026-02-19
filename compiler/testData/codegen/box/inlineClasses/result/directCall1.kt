// WITH_STDLIB

interface I<T> {
    fun foo(x: T): T
}

class C : I<Result<Any?>> {
    override fun foo(x: Result<Any?>) = x
}

fun test() = C().foo(Result.success("OK")).getOrNull()

fun box(): String = test().toString()