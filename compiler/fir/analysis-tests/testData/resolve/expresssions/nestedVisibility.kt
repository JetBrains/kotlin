open class Outer {
    private class PrivateNested
    private inner class PrivateInner

    protected class ProtectedNested
    protected inner class ProtectedInner

    public class PublicNested
    public inner class PublicInner
}

class Derived : Outer() {
    fun foo() {
        Outer.<!HIDDEN{LT}!><!HIDDEN{PSI}!>PrivateNested<!>()<!>
        super.<!HIDDEN{LT}!><!HIDDEN{PSI}!>PrivateInner<!>()<!>

        Outer.ProtectedNested()
        super.ProtectedInner()

        Outer.PublicNested()
        super.PublicInner()
    }
}

fun foo() {
    Outer.<!HIDDEN{LT}!><!HIDDEN{PSI}!>PrivateNested<!>()<!>
    Outer().<!HIDDEN{LT}!><!HIDDEN{PSI}!>PrivateInner<!>()<!>

    Outer.<!HIDDEN{LT}!><!HIDDEN{PSI}!>ProtectedNested<!>()<!>
    Outer().<!HIDDEN{LT}!><!HIDDEN{PSI}!>ProtectedInner<!>()<!>

    Outer.PublicNested()
    Outer().PublicInner()
}
