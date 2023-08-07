fun test1(i: Int) = { <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>i<!> ->
    i
}(i)

fun test2() = { <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>i<!> -> i }<!NO_VALUE_FOR_PARAMETER!>()<!>

fun test3() = { <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>i<!> -> i }(1)
