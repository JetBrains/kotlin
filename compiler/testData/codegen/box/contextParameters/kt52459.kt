// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY

interface I<T>{
    context(a: A)
    fun T.foo(): String
}

class A(val a: String)
class B(val b: String)

object O: I<B>{
    context(a: A)
    override fun B.foo(): String = a.a + this@foo.b
}

fun box(): String {
    with(A("O")){
        with(O as I<B>){
            return B("K").foo()
        }
    }
}