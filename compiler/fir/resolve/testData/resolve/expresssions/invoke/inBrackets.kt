fun test(e: Int.() -> String) {
    val s = 3.<!UNRESOLVED_REFERENCE!>e<!>()
    val ss = 3.(<!UNRESOLVED_REFERENCE!>e<!>)()
}