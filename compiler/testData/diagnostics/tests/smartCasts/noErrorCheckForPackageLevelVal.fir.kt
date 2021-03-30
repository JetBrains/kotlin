//FILE: bar.kt
package bar

val i: Int? = 2

//FILE: foo.kt
package foo

val i: Int? = 1

class A(val i: Int?) {
    fun testUseFromClass() {
        if (foo.i != null) {
            useInt(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
        }
    }
}

fun testUseFromOtherPackage() {
    if (bar.i != null) {
        useInt(<!ARGUMENT_TYPE_MISMATCH!>i<!>)
    }
}

fun useInt(i: Int) = i