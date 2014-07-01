// PARAM_TYPES: Z
// PARAM_DESCRIPTOR: internal fun Z.foo(): kotlin.Int defined in root package
class Z(val a: Int)

// SIBLING:
fun Z.foo(): Int {
    return <selection>this.a + a</selection> + 1
}
