trait A {
    fun foo()
}

trait B {
    fun bar()
}

fun B.b() {
    object : A {
        override fun foo() {
            this@b.bar()
        }
    }
}

fun test() {
    @b { B.() ->
        object : A {
            override fun foo() {
                this@b.bar()
            }
        }
    }
}
