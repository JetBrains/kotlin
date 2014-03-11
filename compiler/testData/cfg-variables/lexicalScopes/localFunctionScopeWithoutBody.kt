fun foo() {
    "before"
    val b = 1
    fun local(x: Int) = x + b
    "after"
}