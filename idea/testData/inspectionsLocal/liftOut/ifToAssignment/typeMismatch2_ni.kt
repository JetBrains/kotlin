// COMPILER_ARGUMENTS: -XXLanguage:+NewInference
// PROBLEM: Assignment should be lifted out of 'if'
// ERROR: None of the following functions can be called with the arguments supplied: <br>public final operator fun plus(other: Byte): Long defined in kotlin.Long<br>public final operator fun plus(other: Double): Double defined in kotlin.Long<br>public final operator fun plus(other: Float): Float defined in kotlin.Long<br>public final operator fun plus(other: Int): Long defined in kotlin.Long<br>public final operator fun plus(other: Long): Long defined in kotlin.Long<br>public final operator fun plus(other: Short): Long defined in kotlin.Long

fun test(b: Boolean, x: Long, y: Long?) {
    var num: Long = 0L
    <caret>if (b) {
        num += x
    } else {
        num += y
    }
}