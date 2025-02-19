// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

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