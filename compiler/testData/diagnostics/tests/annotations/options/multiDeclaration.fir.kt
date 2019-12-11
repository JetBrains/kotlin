@Target(AnnotationTarget.CLASS)
annotation class My
data class Pair(val a: Int, val b: Int)
fun foo(): Int {
    val (@My private a, @My public b) = Pair(12, 34)
    return a + b
}