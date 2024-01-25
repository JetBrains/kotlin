// WITH_STDLIB
// ISSUE: KT-63351

fun test1(x: List<String>?) {
    // x should be non-null in arguments list, despite of a chain
    x?.subList(0, x.size)?.
       subList(0, x.size)?.
       get(x.size)
}

fun test2(x: List<String>?) {
    x?.filter { true }!!.size
    x.size
}
