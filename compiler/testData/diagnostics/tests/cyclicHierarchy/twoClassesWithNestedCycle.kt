open class A : <!CYCLIC_INHERITANCE_HIERARCHY!>B.BB<!>() {
    open class AA
}
open class B : <!CYCLIC_INHERITANCE_HIERARCHY!>A.AA<!>() {
    open class BB
}
