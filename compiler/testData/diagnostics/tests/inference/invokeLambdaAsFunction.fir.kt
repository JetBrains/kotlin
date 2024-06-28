fun test1(i: Int) = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>i<!> ->
    i
}(i)

fun test2() = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>i<!> -> i }<!NO_VALUE_FOR_PARAMETER!>()<!>

fun test3() = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>i<!> -> i }(1)
