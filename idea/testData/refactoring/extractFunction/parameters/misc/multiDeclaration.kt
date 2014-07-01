// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, Any
// PARAM_DESCRIPTOR: val a: kotlin.Int defined in main
// SIBLING:
fun main(args: Array<String>) {
    val (a, b) = Data(1, 2)
    <selection>a</selection>
}

data class Data(val a: Int, val b: Int)