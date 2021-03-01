// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// KT-5068 Add special error for scala-like syntax 'fun foo(): Int = { 1 }'

fun test1(): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }
fun test2(): Int = { 1 }
val test3: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
val test4: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { 1 }<!>
fun test5(): Int { return { 1 } }
fun test6(): Int = fun (): Int = 1

fun outer() {
    fun test1(): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }
    fun test2(): Int = { 1 }
    val test3: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
    val test4: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { 1 }<!>
    fun test5(): Int { return { 1 } }
    fun test6(): Int = fun (): Int = 1
}

class Outer {
    fun test1(): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }
    fun test2(): Int = { 1 }
    val test3: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
    val test4: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { 1 }<!>
    fun test5(): Int { return { 1 } }
    fun test6(): Int = fun (): Int = 1

    class Nested {
        fun test1(): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }
        fun test2(): Int = { 1 }
        val test3: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
        val test4: () -> Int = <!INITIALIZER_TYPE_MISMATCH!>fun (): Int = { 1 }<!>
        fun test5(): Int { return { 1 } }
        fun test6(): Int = fun (): Int = 1
    }
}
