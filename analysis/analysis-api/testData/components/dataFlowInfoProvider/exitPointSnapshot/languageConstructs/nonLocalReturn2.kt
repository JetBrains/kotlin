fun foo(): Int {
    1.let { <expr>return@foo it + 1</expr> }
}