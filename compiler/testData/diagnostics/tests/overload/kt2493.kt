// FIR_IDENTICAL
interface A
interface B

fun <R: A> R.f() {
}

fun <R: B> R.f() {
}

class AImpl: A
class BImpl: B

class C: A, B

fun main() {
    AImpl().f()
    BImpl().f()
    C().<!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
}
