// LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class Components(val x: String)

context(Components)
abstract class A(val y: String) {
    val w: String = x
    fun foo(): String = w + y
}

context(Components)
class B(y: String) : A(y)

fun box(): String {
    return with(Components("O")) {
        B("K").foo()
    }
}
