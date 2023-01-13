fun test(m: Map<String, Int>, mm: MutableMap<Int, String>) {
    m.<!UNRESOLVED_REFERENCE!>getOrDefault<!>("2", 1)
    mm.<!UNRESOLVED_REFERENCE!>getOrDefault<!>(1, "2")
    mm.remove(1, <!TOO_MANY_ARGUMENTS!>"2"<!>)
}
