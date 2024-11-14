// RUN_PIPELINE_TILL: BACKEND
interface Common {
    fun <T, R> foo(value: T, producer: (T) -> R): R
}

class C : B, A {
    override fun <T, R> foo(value: T, producer: (T) -> R): R = null!!
}

interface A : Common {
    override fun <T, R> foo(value: T, producer: (T) -> R): R = null!!
}

interface B : Common {
    override fun <T, R> foo(value: T, producer: (T) -> R) = producer(value)
}
