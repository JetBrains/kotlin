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
    b@ <!UNUSED_FUNCTION_LITERAL!>{ <!DEPRECATED_LAMBDA_SYNTAX!>B.()<!> ->
        object : A {
            override fun foo() {
                this@b.bar()
            }
        }
    }<!>
}