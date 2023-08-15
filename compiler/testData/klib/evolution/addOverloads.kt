// MODULE: lib
// FILE: A.kt
// VERSION: 1

class X {
    fun foo(s: String): String = "initial overload $s"
    fun bar(s: String?): String = "initial nullable overload $s"
    fun qux(s: Any): String = "initial any overload $s"
}

fun foo(s: String): String = "initial overload $s"
fun bar(s: String?): String = "initial nullable overload $s"
fun qux(s: Any): String = "initial any overload $s"

// FILE: B.kt
// VERSION: 2

class X {
    fun foo(): String = "no arg overload"
    fun foo(s: String): String = "initial overload $s"
    fun foo(s: String?): String = "nullable overload $s"
    fun foo(s: String, p: String): String = "more args overload $s"

    fun bar(s: String?): String = "initial overload $s"
    fun bar(s: String): String = "non-nullable overload $s"
    
    fun qux(s: Any): String = "initial any overload $s"
    fun qux(s: String): String = "initial narrower overload $s"
}

fun foo(): String = "no arg overload"
fun foo(s: String): String = "initial overload $s"
fun foo(s: String?): String = "nullable overload $s"
fun foo(s: String, p: String): String = "more args overload $s"

fun bar(s: String?): String = "initial overload $s"
fun bar(s: String): String = "non-nullable overload $s"

fun qux(s: Any): String = "initial any overload $s"
fun qux(s: String): String = "initial narrower overload $s"

// MODULE: mainLib(lib)
// FILE: mainLib.kt

val x = X()

fun lib(): String = when {
    x.foo("first") != "initial overload first" -> "fail 1"
    x.bar("second") != "initial overload second" -> "fail 2"
    x.qux("third") != "initial any overload third" -> "fail 3"

    foo("first") != "initial overload first" -> "fail 4"
    bar("second") != "initial overload second" -> "fail 5"
    qux("third") != "initial any overload third" -> "fail 6"

    else -> "OK"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

