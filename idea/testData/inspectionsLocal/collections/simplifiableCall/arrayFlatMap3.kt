// WITH_RUNTIME
fun test() {
    arrayOf(arrayOf(1, 2), arrayOf(3)).flatMap<caret> { it }
}