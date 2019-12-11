// As in KT-18514
object A : A.I {
    interface I
}

// Similar to 'classIndirectlyInheritsNested.kt'
object D : E() {
    open class NestedD
}

open class E : D.NestedD()



// Similar to 'twoClassesWithNestedCycle.kt'
object G : H.NestedH() {
    open class NestedG
}
object H : G.NestedG() {
    open class NestedH
}

