fun test(
    val f: String.() -> Int = { length }
): Int {
    return "".f()
}