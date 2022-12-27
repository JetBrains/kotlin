// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

interface I<T>{
    context(A)
    fun T.foo(): String
}

class A(val a: String)
class B(val b: String)

object O: I<B>{
    context(A)
    override fun B.foo(): String = this@A.a + this@B.b
}

fun box(): String {
    with(A("O")){
        with(O as I<B>){
            return B("K").foo()
        }
    }
}