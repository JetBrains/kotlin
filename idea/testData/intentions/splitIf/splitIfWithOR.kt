fun foo() {
    val a = true
    val b = false
    if (a <caret>|| b) {
        println("test")
    }
}