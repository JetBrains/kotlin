// WITH_STDLIB

interface I<T> {
    fun foo(): T = TODO()
}

class C : I<Result<Boolean>> {
    override fun foo(): Result<Boolean> = TODO()
}

