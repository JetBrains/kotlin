package sample

actual class <!PACKAGE_OR_CLASSIFIER_REDECLARATION("A")!>A<!> {
    actual fun foo(): Int = 45
    fun fromBottom(): Int = 0
}

fun main() {
    A().foo()

    // Any behaviour is acceptable, as the code is erroneous.
    // At the time of writing this test, we resolve to nearest A, i.e.
    //  'fromBottom' is resolved, and 'fromLeft' is not.
    A().fromLeft()
    A().<!UNRESOLVED_REFERENCE("fromBottom")!>fromBottom<!>()
}