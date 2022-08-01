fun test(f: String.() -> Int = { 45 * <expr>this</expr>.length }) {
    "hello".f()
}