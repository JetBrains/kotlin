class Owner {

    fun foo() {
        bar()
        this.bar()
    }

    fun bar() {
        val i = Inner()
        i.baz()
    }

    fun err() {}

    inner class Inner {
        fun baz() {
            gau()
            this.gau()
        }

        fun gau() {
            val o = Owner()
            o.foo()
            foo()
            this@Owner.foo()
            this.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>err<!>()<!>
        }
    }
}

fun test() {
    val o = Owner()
    o.foo()
    val err = Owner.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>Inner<!>()<!>
    err.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>baz<!>()<!>
    val i = o.Inner()
    i.gau()
}
