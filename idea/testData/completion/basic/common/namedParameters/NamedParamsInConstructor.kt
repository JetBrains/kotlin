package test

class Foo(pFirst: String? = null, val pSecond: Int = 1) { }

fun other() {
    Foo(p<caret>)
}

// EXIST: {"lookupString":"pFirst = ","itemText":"pFirst = "}
// EXIST: {"lookupString":"pSecond = ","itemText":"pSecond = "}