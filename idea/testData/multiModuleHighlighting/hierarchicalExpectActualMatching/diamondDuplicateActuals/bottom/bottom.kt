package sample

actual class A {
    actual fun foo(): Int = 45
    fun fromBottom(): Int = 0
}

fun main() {
    A().foo()

    // Any behaviour is acceptable, as the code is erroneous.
    // At the time of writing this test, we resolve to nearest A, i.e.
    //  'fromBottom' is resolved, and 'fromLeft' is not.
    A().<!UNRESOLVED_REFERENCE("fromLeft")!>fromLeft<!>()
    A().fromBottom()
}