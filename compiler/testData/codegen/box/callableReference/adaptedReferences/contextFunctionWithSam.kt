// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature
class A

fun interface SamInterface {
    context(i: A)
    fun accept(s: String): String
}

fun valueParamFun(a: A, i: String): String { return i }
fun A.extensionFun(i: String): String { return i }

val samObject = SamInterface(::valueParamFun)
val samObject2 = SamInterface(A::extensionFun)

fun box(): String {
    with(A()){
        return samObject.accept("O") + samObject2.accept("K")
    }
}