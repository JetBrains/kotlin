// !CHECK_TYPE
interface A {
    val x: Int

    val z: Comparable<*>
}

open class B {
    open var y = ""

    open val z: CharSequence = ""
}

class C : B(), A {
    override val x
        get() = 1

    override var y
        get() = super.y
        set(value) {
            value checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        }

    override var z
        get() = ""
        set(value) {
            value checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        }
}

fun foo(c: C) {
    c.x checkType { <!UNRESOLVED_REFERENCE!>_<!><Int>() }
    c.y checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }
    c.z checkType { <!UNRESOLVED_REFERENCE!>_<!><String>() }

    c.y = ""
    c.y = 1

    c.z = ""
    c.z = 1
}
