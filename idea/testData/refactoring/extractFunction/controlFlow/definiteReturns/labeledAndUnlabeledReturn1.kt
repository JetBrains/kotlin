// WITH_RUNTIME
// PARAM_DESCRIPTOR: value-parameter it: kotlin.Int defined in foo.<anonymous>
// PARAM_TYPES: kotlin.Int
fun foo(a: Int): Int {
    a.let {
        <selection>if (it > 0) return it else return@foo -it</selection>
    }
    return 0
}