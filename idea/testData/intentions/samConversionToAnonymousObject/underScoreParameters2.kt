fun interface I {
    fun action(a: String, b: Int)
}

fun test() {
   <caret>I { _, _ -> }
}