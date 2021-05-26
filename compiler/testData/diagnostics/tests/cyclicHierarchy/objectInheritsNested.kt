// FIR_IDENTICAL
// As in KT-18514
object A : <!CYCLIC_INHERITANCE_HIERARCHY!>A.I<!> {
    interface I
}

// Similar to 'classIndirectlyInheritsNested.kt'
object D : <!CYCLIC_INHERITANCE_HIERARCHY!>E<!>() {
    open class NestedD
}

open class E : <!CYCLIC_INHERITANCE_HIERARCHY!>D.NestedD<!>()



// Similar to 'twoClassesWithNestedCycle.kt'
object G : <!CYCLIC_INHERITANCE_HIERARCHY!>H.NestedH<!>() {
    open class NestedG
}
object H : <!CYCLIC_INHERITANCE_HIERARCHY!>G.NestedG<!>() {
    open class NestedH
}

