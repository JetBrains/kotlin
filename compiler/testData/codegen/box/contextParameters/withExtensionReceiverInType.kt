// IGNORE_BACKEND_K1: ANY
// IGNORE_BACKEND_K2: ANY
// IGNORE_IR_DESERIALIZATION_TEST: ANY
// ISSUE: KT-74081
// LANGUAGE: +ContextParameters

class C(val a: String) {
    fun foo(): String {
        return a
    }
}

context(ctx: T)
fun <T> implicit(): T = ctx

fun test1(x: context(C) C.(y: C) -> String): String {
    with(C("!")) {
        return C("O").x(C("K"))
    }
}

fun test2(x: context(C) C.(y: C) -> String): String {
    with(C("!")) {
        return x(C("O"), C("K"))
    }
}

fun test3(x: context(C) C.(y: C) -> String): String {
    return x(C("!"), C("O"), C("K"))
}

val property1: context(C) C.(C)-> String
    get()  = { y -> this.foo() + y.foo() + implicit<C>().foo() }

fun test4(): context(C) C.(C)-> String {
    return { y -> this.foo() + y.foo() + implicit<C>().foo() }
}

fun box(): String {
    return if ((test1 { a: C -> this.foo() + a.foo() + implicit<C>().foo() } == "OK!") &&
        (test2 { a: C -> this.foo() + a.foo() + implicit<C>().foo() } == "OK!") &&
        (test3 { a: C -> this.foo() + a.foo() + implicit<C>().foo() } == "OK!") &&
        (property1(C("!"), C("O"), C("K")) == "OK!") &&
        (with(C("!")){ property1( C("O"), C("K")) == "OK!")}) &&
        (test4()(C("!"), C("O"), C("K")) == "OK!") &&
        (with(C("!")){ test4()( C("O"), C("K")) == "OK!")})) "OK"
    else "NOK"
}
