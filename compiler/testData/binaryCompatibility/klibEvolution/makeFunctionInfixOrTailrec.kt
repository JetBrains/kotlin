// MODULE: lib
// FILE: A.kt
// VERSION: 1

fun X.foo(param: String) = "foo before change $param"
fun bar(param: String) = "bar before change $param"

class X() {
    fun qux(param: String) = "qux before change $param"
    fun muc(param: String) = "muc before change $param"
}

// FILE: B.kt
// VERSION: 2

infix fun X.foo(param: String) = "foo after change to infix $param"
tailrec fun bar(param: String) = "bar after change to tailrec $param"

class X() {
    infix fun qux(param: String) = "qux after change to infix $param"
    tailrec fun muc(param: String) = "muc after change to tailrec $param"
}


// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String = when {
    X().foo("yes") != "foo after change to infix yes" -> "fail 1"
    bar("indeed") != "bar after change to tailrec indeed" -> "fail 2"
    X().qux("naturally") != "qux after change to infix naturally" -> "fail 3"
    X().muc("of course") != "muc after change to tailrec of course" -> "fail 4"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

