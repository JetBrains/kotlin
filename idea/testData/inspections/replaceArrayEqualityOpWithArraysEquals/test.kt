fun foo() {
    val a = arrayOf("a")
    val b = a
    val c: Any? = null
    a == b // YES
    a == c // NO
}