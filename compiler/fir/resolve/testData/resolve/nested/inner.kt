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
            this.err()
        }
    }
}

fun test() {
    val o = Owner()
    o.foo()
    val err = Owner.Inner()
    err.baz()
    val i = o.Inner()
    i.gau()
}