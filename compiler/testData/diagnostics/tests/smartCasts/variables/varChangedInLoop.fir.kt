public fun foo() {
    var i: Any = 1
    if (i is Int) {
        while (i != 10) {
            i<!UNRESOLVED_REFERENCE!>++<!>      // Here smart cast should not be performed due to a successor
            i = ""
        }
    }
}
