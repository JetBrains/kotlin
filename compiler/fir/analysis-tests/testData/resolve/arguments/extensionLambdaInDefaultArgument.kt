fun test(
    f: String.() -> Int = { length }
): Int {
    return "".f()
}
