// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

class Components(val x: String)

context(Components)
abstract class A<F : CharSequence>(val y: F) {
    fun foo(): String = x + y
}

context(Components)
class B(y: String) : A<String>(y)

fun box(): String {
    return with(Components("O")) {
        B("K").foo()
    }
}
