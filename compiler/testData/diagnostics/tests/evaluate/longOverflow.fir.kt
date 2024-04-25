// DIAGNOSTICS: -UNUSED_VARIABLE

val longMaxValue: Long = 0x7fffffffffffffff
val longMinValue: Long = -longMaxValue - 1
val intMaxValue: Int = 0x7fffffff
val intMinValue: Int = 1 shl 31

val a2: Long = longMinValue - 10

val l1: Long = longMaxValue + 1
val l2: Long = longMaxValue - 1 + 2
val l3: Long = longMaxValue - longMinValue
val l4: Long = -longMinValue
val l5: Long = longMinValue - 1
val l6: Long = longMinValue - longMaxValue
val l7: Long = longMinValue + longMaxValue
val l8: Long = -longMaxValue
val l10: Long = -intMinValue.toLong()
val l11: Long = -1 + intMinValue.toLong()
val l12: Long = longMinValue * intMinValue
val l13: Long = longMinValue * -1
val l14: Long = longMinValue * 2
val l15: Long = longMaxValue * -2
val l16: Long = intMinValue.toLong() * -1
val l19: Long = longMinValue / -1

fun foo() {
    val l1: Long = longMaxValue + 1
    val l2: Long = longMaxValue - 1 + 2
    val l3: Long = longMaxValue - longMinValue
    val l4: Long = -longMinValue
    val l5: Long = longMinValue - 1
    val l6: Long = longMinValue - longMaxValue
    val l7: Long = longMinValue + longMaxValue
    val l8: Long = -longMaxValue
    val l10: Long = -intMinValue.toLong()
    val l11: Long = -1 + intMinValue.toLong()
    val l12: Long = longMinValue * intMinValue
    val l13: Long = longMinValue * -1
    val l14: Long = longMinValue * 2
    val l15: Long = longMaxValue * -2
    val l16: Long = intMinValue.toLong() * -1
    val l19: Long = longMinValue / -1
}

class A {
    fun foo() {
        val l1: Long = longMaxValue + 1
        val l2: Long = longMaxValue - 1 + 2
        val l3: Long = longMaxValue - longMinValue
        val l4: Long = -longMinValue
        val l5: Long = longMinValue - 1
        val l6: Long = longMinValue - longMaxValue
        val l7: Long = longMinValue + longMaxValue
        val l8: Long = -longMaxValue
        val l10: Long = -intMinValue.toLong()
        val l11: Long = -1 + intMinValue.toLong()
        val l12: Long = longMinValue * intMinValue
        val l13: Long = longMinValue * -1
        val l14: Long = longMinValue * 2
        val l15: Long = longMaxValue * -2
        val l16: Long = intMinValue.toLong() * -1
        val l19: Long = longMinValue / -1
    }
}