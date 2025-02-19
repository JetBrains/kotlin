// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-73779
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature
class A

class Base {
    context(a: A)
    fun funMember(): String { return "OK" }
}

context(a: A)
fun Base.funMember(): String { return "not OK" }

fun box(): String {
    with(A()) {
        return Base().funMember()
    }
}