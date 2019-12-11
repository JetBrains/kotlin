public fun foo() {
    var i: Int? = 1
    if (i != null) {
        while (i != 10) {
            i++      // Here smart cast should not be performed due to a successor
            i = null
        }
    }
}