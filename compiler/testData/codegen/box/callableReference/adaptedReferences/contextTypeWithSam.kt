// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

class C(var a: String) {
    fun foo(): String { return a }
}

context(ctx: T)
fun <T> implicit(): T = ctx

fun interface SamInterface {
    context(i: C)
    fun accept(): String
}

val foo: context(C) () -> String
    get() = { implicit<C>().foo() }

val samObject = SamInterface(::foo.get())

fun box(): String {
    with(C("OK")) {
        return samObject.accept()
    }
}