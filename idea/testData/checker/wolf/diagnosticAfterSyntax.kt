// WOLF-ERRORS: true
// HAS-WOLF-ERRORS: true
// TYPE: asd
// ERROR: Overload resolution ambiguity: <br>public final operator fun plus(other: Byte): Int defined in kotlin.Int<br>public final operator fun plus(other: Double): Double defined in kotlin.Int<br>public final operator fun plus(other: Float): Float defined in kotlin.Int<br>public final operator fun plus(other: Int): Int defined in kotlin.Int<br>public final operator fun plus(other: Long): Long defined in kotlin.Int<br>public final operator fun plus(other: Short): Int defined in kotlin.Int
fun diagnosticAfterSyntax(): Int = 42 + <caret>