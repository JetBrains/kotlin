// ISSUE: KT-71966

fun foo() {
    open class Local {
        open inner class A : <!CYCLIC_INHERITANCE_HIERARCHY!>C<!>() {
            abstract inner class Inner
        }

        abstract inner class C : <!CYCLIC_INHERITANCE_HIERARCHY, INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>A.Inner<!>() // INNER_CLASS_CONSTRUCTOR_NO_RECEIVER
    }
}
