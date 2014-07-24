// PARAM_TYPES: kotlin.Int, Comparable<Int>
// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, Any
// PARAM_DESCRIPTOR: value-parameter val a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>if(a > 0) {
        println(a)
    }
    println(b)</selection>
}