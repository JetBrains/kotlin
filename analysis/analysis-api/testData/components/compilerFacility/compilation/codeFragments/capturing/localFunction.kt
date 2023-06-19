fun test() {
    fun call(a: Int, b: String) {
        println(b.repeat(a))
    }

    val x = 2
    val y = "foo"
    <caret>val z = Unit
}