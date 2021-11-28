

open class A {
    open fun foo(): A = this
    open fun bar(): A = this
    open fun buz(p: A): A = this
}

class B : A() {
    override fun foo(): B = this
    fun <!VIRTUAL_MEMBER_HIDDEN!>bar<!>(): B = this // Missing 'override'
    <!NOTHING_TO_OVERRIDE!>override<!> fun buz(p: B): B = this //No override as B not :> A

    fun test() {
        foo()
        bar()
        <!NONE_APPLICABLE!>buz<!>()
    }
}

