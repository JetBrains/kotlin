// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

class C(var a: String) {
    fun foo(): String { return a }
}

fun interface SamInterface {
    context(i: C)
    fun accept(): String
}

val C.expensionProperty: String
    get() = this.foo()

val samObject = SamInterface(C::expensionProperty)

fun box(): String {
    with(C("OK")){
        return samObject.accept()
    }
}