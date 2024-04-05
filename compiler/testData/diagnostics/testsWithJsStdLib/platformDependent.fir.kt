fun test(m: Map<String, Int>, mm: MutableMap<Int, String>) {
    m.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>"2"<!>, <!TOO_MANY_ARGUMENTS!>1<!>)
    mm.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>1<!>, <!TOO_MANY_ARGUMENTS!>"2"<!>)
    mm.remove(1, <!TOO_MANY_ARGUMENTS!>"2"<!>)
}
