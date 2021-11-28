// MODULE: lib
// FILE: A.kt
// VERSION: 1

fun foo(param: String) = "foo before change $param"

class X(val constructorParam: String) {
    fun bar(param: String) = "bar before change $param and $constructorParam"
}

// FILE: A.kt
// VERSION: 2

fun foo(param: String = "none") = "foo after change $param"

class X(val constructorParam: String = "none") {
    fun bar(param: String = "none") = "bar after change $param and $constructorParam"
}


// MODULE: mainLib(lib)
// FILE: mainLib.kt
fun lib(): String = when {
    foo("global") != "foo after change global" -> "fail 1"
    X("constructor").bar("member") != "bar after change member and constructor" -> "fail 2"
    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

