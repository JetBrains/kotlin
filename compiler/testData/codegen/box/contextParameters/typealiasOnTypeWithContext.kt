// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

typealias TypeAliasOnContextFunType = context(A) (String) -> String

val a: TypeAliasOnContextFunType = { y: String -> implicit<A>().foo(y) }

fun box(): String {
    return a(A(),"OK")
}

