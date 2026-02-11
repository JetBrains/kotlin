// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

class Components(val x: String)

context(Components)
abstract class A {
    fun foo(): String = x
}

context(Components)
class B : A()

fun box(): String {
    return with(Components("OK")) {
        B().foo()
    }
}
