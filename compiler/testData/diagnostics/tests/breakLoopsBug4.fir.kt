// ISSUE: KT-71966

fun foo() {
    open class Local {
        open inner class A : C() {
            abstract inner class Inner
        }

        abstract inner class C : <!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>A.Inner<!>() // INNER_CLASS_CONSTRUCTOR_NO_RECEIVER
    }
}
