interface A {
    val s: String
}

fun test(list: List<A>) {
    var goodA: A? = null
    for (a in list) {
        if (goodA == null) {
            goodA = a
            continue
        }
        goodA.<!INAPPLICABLE_CANDIDATE!>s<!>
    }
}
