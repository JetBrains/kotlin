//FILE: bar.kt
package bar

val i: Int? = 2

//FILE: foo.kt
package foo

val i: Int? = 1

class A(val i: Int?) {
    fun testUseFromClass() {
        if (foo.i != null) {
            <!INAPPLICABLE_CANDIDATE!>useInt<!>(i)
        }
    }
}

fun testUseFromOtherPackage() {
    if (bar.i != null) {
        <!INAPPLICABLE_CANDIDATE!>useInt<!>(i)
    }
}

fun useInt(i: Int) = i