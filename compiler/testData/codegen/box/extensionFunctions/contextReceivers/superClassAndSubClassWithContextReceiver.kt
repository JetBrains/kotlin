// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

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
