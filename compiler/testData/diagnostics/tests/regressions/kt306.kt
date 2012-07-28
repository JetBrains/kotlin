// KT-306 Ambiguity when different this's have same-looking functions

fun test() {
    <!UNUSED_FUNCTION_LITERAL!>{Foo.() ->
        bar();
        {Barr.() ->
            this.bar()
            bar()
        }
    }<!>
    <!UNUSED_FUNCTION_LITERAL!>{Barr.() ->
        bar()
    }<!>
}

class Foo {
    fun bar() {}
}

class Barr {
    fun bar() {}
}
