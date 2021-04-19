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
        Outer.<!INVISIBLE_REFERENCE!>PrivateNested<!>()
        super.<!INVISIBLE_REFERENCE!>PrivateInner<!>()

        Outer.ProtectedNested()
        super.ProtectedInner()

        Outer.PublicNested()
        super.PublicInner()
    }
}

fun foo() {
    Outer.<!INVISIBLE_REFERENCE!>PrivateNested<!>()
    Outer().<!INVISIBLE_REFERENCE!>PrivateInner<!>()

    Outer.<!INVISIBLE_REFERENCE!>ProtectedNested<!>()
    Outer().<!INVISIBLE_REFERENCE!>ProtectedInner<!>()

    Outer.PublicNested()
    Outer().PublicInner()
}
