// IGNORE_BACKEND_K1: ANY

fun box(): String {
    return "K".(fun String.(): String.()-> String = { a: String -> a + this })()("O")
}