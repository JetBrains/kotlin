open class C : <!CYCLIC_INHERITANCE_HIERARCHY!>D<!>() {
    open class CC
}
open class D : <!CYCLIC_INHERITANCE_HIERARCHY!>C.CC<!>()
