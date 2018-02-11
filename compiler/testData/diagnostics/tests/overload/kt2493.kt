// !WITH_NEW_INFERENCE
interface A
interface B

fun <R: A> R.f() {
}

fun <R: B> R.f() {
}

class AImpl: A
class BImpl: B

class C: A, B

fun main(args: Array<String>) {
    AImpl().f()
    BImpl().f()
    C().<!NI;OVERLOAD_RESOLUTION_AMBIGUITY, OI;CANNOT_COMPLETE_RESOLVE!>f<!>()
}