// IGNORE_BACKEND_FIR: JVM_IR
interface A
interface B

class C : A, B

fun <T> T.foo(): String where T : A, T : B {
    return "OK"
}

fun box(): String {
    return C().foo()
}