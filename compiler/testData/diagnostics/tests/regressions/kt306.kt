// KT-306 Ambiguity when different this's have same-looking functions

fun test() {
    (<!UNUSED_EXPRESSION!>fun Foo.() {
        bar()
        (<!UNUSED_EXPRESSION!>fun Barr.() {
            this.bar()
            bar()
        }<!>)
    }<!>)
    (<!UNUSED_EXPRESSION!>fun Barr.() {
        this.bar()
        bar()
    }<!>)
}

class Foo {
    fun bar() {}
}

class Barr {
    fun bar() {}
}
