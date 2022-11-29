fun test1(i: Int) = { i ->
    i
}(i)

fun test2() = <!NO_VALUE_FOR_PARAMETER!>{ i -> i }()<!>

fun test3() = { i -> i }(1)
