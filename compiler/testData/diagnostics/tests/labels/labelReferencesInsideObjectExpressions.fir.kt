interface A {
    fun foo()
}

interface B {
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
    fun <T> without(f: T.() -> Unit): Unit = (null!!).f()
    without<B>() b@ {
        object : A {
            override fun foo() {
                this@b.bar()
            }
        }
    }
}