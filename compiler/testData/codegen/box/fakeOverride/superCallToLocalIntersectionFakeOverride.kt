// ISSUE: KT-91111
// A super call to an intersection fake override of a local class must reach the implementation inherited from
// the superclass, even if an abstract member of an interface comes first in the list of supertypes.

interface I {
    fun foo(): String
}

interface IP {
    val p: String
}

open class A {
    open fun foo(): String = "foo"
    open val p: String get() = "p"
}

fun box(): String {
    open class InterfaceFirst : I, IP, A()
    open class ClassFirst : A(), I, IP

    class OverridesInterfaceFirst : InterfaceFirst() {
        override fun foo(): String = super.foo()
        override val p: String get() = super.p
    }

    class OverridesClassFirst : ClassFirst() {
        override fun foo(): String = super.foo()
        override val p: String get() = super.p
    }

    val anonymousInterfaceFirst = object : InterfaceFirst() {
        override fun foo(): String = super.foo()
        override val p: String get() = super.p
    }

    if (OverridesInterfaceFirst().foo() != "foo") return "Fail 1"
    if (OverridesInterfaceFirst().p != "p") return "Fail 2"
    if (OverridesClassFirst().foo() != "foo") return "Fail 3"
    if (OverridesClassFirst().p != "p") return "Fail 4"
    if (anonymousInterfaceFirst.foo() != "foo") return "Fail 5"
    if (anonymousInterfaceFirst.p != "p") return "Fail 6"

    return "OK"
}
