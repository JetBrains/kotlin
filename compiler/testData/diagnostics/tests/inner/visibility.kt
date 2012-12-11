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
        Outer.<!INVISIBLE_MEMBER!>PrivateNested<!>()
        super.<!INVISIBLE_MEMBER!>PrivateInner<!>()

        Outer.ProtectedNested()
        super.ProtectedInner()

        Outer.PublicNested()
        super.PublicInner()
    }
}

fun foo() {
    Outer.<!INVISIBLE_MEMBER!>PrivateNested<!>()
    Outer().<!INVISIBLE_MEMBER!>PrivateInner<!>()

    Outer.<!INVISIBLE_MEMBER!>ProtectedNested<!>()
    Outer().<!INVISIBLE_MEMBER!>ProtectedInner<!>()

    Outer.PublicNested()
    Outer().PublicInner()
}
