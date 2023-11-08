// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-54139

class A
class B
fun B.extensionFunction() {}

context(A, B)
fun test() {
    extensionFunction()
}

fun box(): String {
    with(A()) {
        with(B()) {
            test()
        }
    }
    return "OK"
}
