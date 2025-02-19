// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature
class A

fun funWithContextType(x: context(A) () -> A) {}
inline fun inlineFunWithContextType(x: context(A) () -> A) {}

val A.extensionProperty: A
    get() = A()

val <T> T.extensionPropertyWithTypeParam: T
    get() = A() as T

fun box(): String {
    funWithContextType(A::extensionProperty)
    funWithContextType(A::extensionPropertyWithTypeParam)
    inlineFunWithContextType(A::extensionProperty)
    inlineFunWithContextType(A::extensionPropertyWithTypeParam)
    return "OK"
}