// MODULE: lib
// FILE: B.kt
// VERSION: 1

infix fun X.foo(param: String) = "foo before change $param"
tailrec fun bar(param: String) = "bar before change $param"

class X() {
    infix fun qux(param: String) = "qux before change $param"
    tailrec fun muc(param: String) = "muc before change $param"
}

// FILE: A.kt
// VERSION: 2

fun X.foo(param: String) = "foo after removal of infix $param"
fun bar(param: String) = "bar after removal of tailrec $param"

class X() {
    fun qux(param: String) = "qux after removal of infix $param"
    fun muc(param: String) = "muc after removal of tailrec $param"
}


// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String = when {
    X() foo "yes" != "foo after removal of infix yes" -> "fail 1"
    bar("indeed") != "bar after removal of tailrec indeed" -> "fail 2"
    X() qux "naturally" != "qux after removal of infix naturally" -> "fail 3"
    X().muc("of course") != "muc after removal of tailrec of course" -> "fail 4"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

