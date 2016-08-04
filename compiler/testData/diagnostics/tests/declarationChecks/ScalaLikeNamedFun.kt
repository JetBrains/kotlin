// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// KT-5068 Add special error for scala-like syntax 'fun foo(): Int = { 1 }'

fun test1(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
fun test2(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
val test3: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
val test4: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
fun test5(): Int { return <!TYPE_MISMATCH!>{ 1 }<!> }
fun test6(): Int = <!TYPE_MISMATCH!>fun (): Int = 1<!>

fun outer() {
    fun test1(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
    fun test2(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
    val test3: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
    val test4: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
    fun test5(): Int { return <!TYPE_MISMATCH!>{ 1 }<!> }
    fun test6(): Int = <!TYPE_MISMATCH!>fun (): Int = 1<!>
}

class Outer {
    fun test1(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
    fun test2(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
    val test3: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
    val test4: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
    fun test5(): Int { return <!TYPE_MISMATCH!>{ 1 }<!> }
    fun test6(): Int = <!TYPE_MISMATCH!>fun (): Int = 1<!>

    class Nested {
        fun test1(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
        fun test2(): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
        val test3: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ <!RETURN_NOT_ALLOWED!>return<!> 1 }<!>
        val test4: () -> Int = fun (): Int = <!TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN!>{ 1 }<!>
        fun test5(): Int { return <!TYPE_MISMATCH!>{ 1 }<!> }
        fun test6(): Int = <!TYPE_MISMATCH!>fun (): Int = 1<!>
    }
}
