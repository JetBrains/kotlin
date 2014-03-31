// IS_APPLICABLE: false
fun foo() {
    val a = true
    val b = false
    when (<caret>a && b) {
        println("test")
    }
}