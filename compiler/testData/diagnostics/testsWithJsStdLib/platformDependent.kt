fun test(m: Map<String, Int>, mm: MutableMap<Int, String>) {
    m.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getOrDefault<!>("2", 1)
    mm.<!DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getOrDefault<!>(1, "2")
    mm.remove(1, <!TOO_MANY_ARGUMENTS!>"2"<!>)
}
