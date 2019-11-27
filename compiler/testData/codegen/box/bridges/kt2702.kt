// IGNORE_BACKEND_FIR: JVM_IR
open class A<R> {
    open fun foo(r: R): R {return r}
}

open class B : A<String>() {
}

open class C : B() {
    override fun foo(r: String): String {
        return super.foo(r) + "K"
    }
}

fun box() = C().foo("O")
