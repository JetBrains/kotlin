// WITH_STDLIB
fun <T> consume(action: (T) -> Unit) {}

fun test() {
    val explicit = { x: Int, y: String -> x }
    consume<String> { s -> }
    consume<Pair<Int, String>> { (a, b) -> }
    consume<Pair<Int, String>> { (a: Int, _) -> }
    val extension = fun Int.(x: Int): String = "$this$x"
}
