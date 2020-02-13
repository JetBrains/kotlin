fun test(m: Map<String, Int>, mm: MutableMap<Int, String>) {
    m.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getOrDefault<!>("2", 1)
    mm.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getOrDefault<!>(1, "2")
    mm.remove(1, <!TOO_MANY_ARGUMENTS!>"2"<!>)
}
