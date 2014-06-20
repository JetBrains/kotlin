// PARAM_TYPES: kotlin.Int, Comparable<Int>
// PARAM_TYPES: kotlin.Int, Number, Comparable<Int>, Any
// SIBLING:
fun foo(a: Int) {
    val b: Int = 1

    <selection>if(a > 0) {
        println(a)
    }
    println(b)</selection>
}