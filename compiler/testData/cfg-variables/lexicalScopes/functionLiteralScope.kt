fun foo() {
    "before"
    val b = 1
    val f = { x: Int ->
        val a = x + b
    }
    "after"
}