class Z(val a: Int)

// NEXT_SIBLING:
fun Z.foo(): Int {
    return <selection>this.a</selection> + 1
}
