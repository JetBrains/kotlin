fun box(): String {
    return "O".(fun String.(y: String): String = this + y)("K")
}