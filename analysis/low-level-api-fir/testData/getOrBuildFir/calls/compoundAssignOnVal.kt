interface A {
    operator fun plusAssign(i: Int)
}
fun test(l: A) {
    <expr>l += 1</expr>
}