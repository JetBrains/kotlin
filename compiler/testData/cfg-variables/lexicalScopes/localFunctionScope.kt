fun foo() {
    "before"
    val b = 1
    fun local(x: Int) {
        val a = x + b
    }
    "after"
}