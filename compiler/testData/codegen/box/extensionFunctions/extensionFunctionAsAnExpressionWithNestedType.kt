fun box(): String {
    return "K".(fun String.(): (String) -> String = { a: String -> a + this })()("O")
}
