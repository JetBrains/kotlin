interface A {
    operator fun plusAssign(i: Int)
}
fun test(l: A) {
    <expr>l</expr> += 1
}