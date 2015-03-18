// KT-306 Ambiguity when different this's have same-looking functions

fun test() {
    <!UNUSED_FUNCTION_LITERAL!>{<!DEPRECATED_LAMBDA_SYNTAX!>Foo.()<!> ->
        bar();
        {<!DEPRECATED_LAMBDA_SYNTAX!>Barr.()<!> ->
            this.bar()
            bar()
        }
    }<!>
    <!UNUSED_FUNCTION_LITERAL!>{<!DEPRECATED_LAMBDA_SYNTAX!>Barr.()<!> ->
        bar()
    }<!>
}

class Foo {
    fun bar() {}
}

class Barr {
    fun bar() {}
}
