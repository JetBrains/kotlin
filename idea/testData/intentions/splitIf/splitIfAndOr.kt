// IS_APPLICABLE: false
fun foo() {
    val a = true
    val b = false
    val c = true
    if (a <caret>&& b || c) {
        println("test")
    }
}