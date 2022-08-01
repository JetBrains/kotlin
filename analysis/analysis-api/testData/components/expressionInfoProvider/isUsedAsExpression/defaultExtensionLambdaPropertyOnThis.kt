fun test(f: String.() -> Int = { 45 * this.<expr>length</expr> }) {
    "hello".f()
}