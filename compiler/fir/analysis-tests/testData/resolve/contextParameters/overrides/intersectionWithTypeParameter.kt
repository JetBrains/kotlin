// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
class A

interface First<T> {
    context(a: T)
    fun foo()

    context(a: T)
    val b: String
}

interface Second<K> {
    context(a: K)
    fun foo()

    context(a: K)
    val b: String
}

interface IntersectionInterface<T> : First<T>, Second<T>

class IntersectionClass<R> : First<R>, Second<R> {
    context(a: R)
    override fun foo() { }

    context(a: R)
    override val b: String
        get() = "2"
}

fun usage(a: IntersectionInterface<A>){
    with(A()) {
        a.foo()
        IntersectionClass<A>().foo()
    }
}