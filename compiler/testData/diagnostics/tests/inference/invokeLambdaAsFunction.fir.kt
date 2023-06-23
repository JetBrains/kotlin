fun test1(i: Int) = { i ->
    i
}(i)

fun test2() = { i -> i }<!NO_VALUE_FOR_PARAMETER!>()<!>

fun test3() = { i -> i }(1)
