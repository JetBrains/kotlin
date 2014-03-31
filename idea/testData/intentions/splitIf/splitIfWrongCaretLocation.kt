// IS_APPLICABLE: false
fun foo() {
    val a = true
    val b = false
    if (<caret>a && b) {
        println("test")
    }
}