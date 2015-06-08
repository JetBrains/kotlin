// fun foo:     abstract in A,      unresolved in I
// fun bar:     implemented in A,   abstract in I
// fun qux:     abstract in A,      abstract in I
// val x:       unresolved in A,    abstract in I
// val y:       abstract in A,      implemented in I

abstract class A {
    abstract fun foo(): Int
    open fun bar() {}
    abstract fun qux()

    abstract val y: Int
}

interface I {
    fun bar()
    fun qux()

    val x: Int
    val y: Int get() = 111
}

class B : A(), I {
    override val x: Int = 12345
    override val y: Int = super.y

    override fun foo(): Int {
        super.<!ABSTRACT_SUPER_CALL!>foo<!>()
        return super.<!ABSTRACT_SUPER_CALL!>x<!>
    }

    override fun bar() {
        super.bar()
    }

    override fun qux() {
        <!AMBIGUOUS_SUPER!>super<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>qux<!>()
    }
}
