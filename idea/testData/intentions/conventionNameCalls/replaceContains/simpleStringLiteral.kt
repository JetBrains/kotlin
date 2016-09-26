public operator fun CharSequence.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean = false
fun test() {
    val foo = "foo"
    foo.<caret>contains("bar")
}
