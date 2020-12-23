// MODULE: lib
// FILE: A.kt
// VERSION: 1

fun foo(param: String = "global") = "foo before change $param"

class X(val constructorParam: String = "constructor") {
    fun bar(param: String = "member") = "bar before change $param and $constructorParam"
}

// FILE: B.kt
// VERSION: 2

fun foo(param: String = "in file") = "foo after change $param"

class X(val constructorParam: String = "in constructor") {
    fun bar(param: String = "in class") = "bar after change $param and $constructorParam"
}

// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String = when {
    foo("in global") != "foo after change in global" -> "fail 1"
    X("in constructor").bar("in member") != "bar after change in member and in constructor" -> "fail 2"
    foo() != "foo after change in file" -> "fail 3"
    X().bar() != "bar after change in class and in constructor" -> "fail 4"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

