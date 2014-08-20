// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: val m: kotlin.Int defined in foo
// EXTRACT_AS_PROPERTY

val n: Int = 1

// SIBLING:
fun foo(): Int {
    val m = 1
    return <selection>n + m + 1</selection>
}