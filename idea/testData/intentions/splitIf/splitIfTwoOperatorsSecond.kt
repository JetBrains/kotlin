fun foo() {
    val a = true
    val b = false
    val c = true
    if (a && b &<caret>& c) {
        println("test")
    }
}