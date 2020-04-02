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
        Outer.<!INAPPLICABLE_CANDIDATE!>PrivateNested<!>()
        super.<!INAPPLICABLE_CANDIDATE!>PrivateInner<!>()

        Outer.ProtectedNested()
        super.ProtectedInner()

        Outer.PublicNested()
        super.PublicInner()
    }
}

fun foo() {
    Outer.<!INAPPLICABLE_CANDIDATE!>PrivateNested<!>()
    Outer().<!INAPPLICABLE_CANDIDATE!>PrivateInner<!>()

    Outer.<!INAPPLICABLE_CANDIDATE!>ProtectedNested<!>()
    Outer().<!INAPPLICABLE_CANDIDATE!>ProtectedInner<!>()

    Outer.PublicNested()
    Outer().PublicInner()
}
