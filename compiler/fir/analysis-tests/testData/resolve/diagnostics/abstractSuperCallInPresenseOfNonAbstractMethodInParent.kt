// ISSUE: KT-38400

interface IWithToString {
    override fun toString(): String
    fun foo(): String
    fun bar(): String
}

open class B {
    open fun foo(): String = ""
}

class A : IWithToString, B() {
    override fun toString(): String = super.toString() // resolve to Any.toString
    override fun foo(): String = super.foo() // resolve to B.foo()
    override fun bar(): String = super.<!ABSTRACT_SUPER_CALL!>bar<!>() // should be an error
}
