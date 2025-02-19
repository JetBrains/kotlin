// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

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

