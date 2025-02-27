// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
open class A {
    fun foo(a: String): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

fun interface SamInterface {
    context(i: A)
    fun accept(s: String): String
}

val samObject = SamInterface { s: String -> implicit<A>().foo(s) }

fun box(): String {
    with(A()){
        return samObject.accept("OK")
    }
}