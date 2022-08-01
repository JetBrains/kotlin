fun test(f: String.() -> Int = { 45 * <expr>this.length</expr> }) {
    "hello".f()
}