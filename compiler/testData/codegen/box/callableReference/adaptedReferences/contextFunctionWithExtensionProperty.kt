// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt

class A

inline fun inlineFunWithContextType(x: context(A) () -> A) {}

// FILE: main.kt

fun funWithContextType(x: context(A) () -> A) {}

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