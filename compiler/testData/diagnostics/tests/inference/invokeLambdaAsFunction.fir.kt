fun test1(i: Int) = <!UNRESOLVED_REFERENCE!>{ i ->
    i
}(i)<!>

fun test2() = <!UNRESOLVED_REFERENCE!>{ i -> i }()<!>

fun test3() = <!UNRESOLVED_REFERENCE!>{ i -> i }(1)<!>