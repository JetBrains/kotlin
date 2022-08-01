fun test(f: String.() -> Int = <expr>{ 45 * this.length }</expr>) {
    "hello".f()
}