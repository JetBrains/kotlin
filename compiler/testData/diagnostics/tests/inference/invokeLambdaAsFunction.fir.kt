fun test1(i: Int) = { i ->
    i
}(i)

fun test2() = <!INAPPLICABLE_CANDIDATE!>{ i -> i }<!>()

fun test3() = { i -> i }(1)