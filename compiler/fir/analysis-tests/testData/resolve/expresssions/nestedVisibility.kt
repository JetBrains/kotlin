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
        Outer.<!HIDDEN!>PrivateNested<!>()
        super.<!HIDDEN!>PrivateInner<!>()

        Outer.ProtectedNested()
        super.ProtectedInner()

        Outer.PublicNested()
        super.PublicInner()
    }
}

fun foo() {
    Outer.<!HIDDEN!>PrivateNested<!>()
    Outer().<!HIDDEN!>PrivateInner<!>()

    Outer.<!HIDDEN!>ProtectedNested<!>()
    Outer().<!HIDDEN!>ProtectedInner<!>()

    Outer.PublicNested()
    Outer().PublicInner()
}
