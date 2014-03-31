// IS_APPLICABLE: false
fun foo() {
    val a = true
    val b = false
    fun test(a: Boolean, b: Boolean): Boolean { return false }
    if (test(a &<caret>& b)) {
        println("test")
    }
}