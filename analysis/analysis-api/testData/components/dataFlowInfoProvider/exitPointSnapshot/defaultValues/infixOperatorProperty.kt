val foo: String.(String) -> Unit = { _ -> }

fun test() {
    "A" <expr>foo</expr> "B"
}