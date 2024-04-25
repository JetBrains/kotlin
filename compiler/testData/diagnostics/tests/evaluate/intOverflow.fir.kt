// DIAGNOSTICS: -UNUSED_VARIABLE

val intMaxValue: Int = 0x7fffffff
val intMinValue: Int = 1 shl 31

val a3: Int = intMaxValue + 1 - 10
val a4: Int = intMaxValue + 1 + 10
val i2: Int = intMaxValue - 1 + 2
val i3: Int = intMaxValue - intMinValue
val i4: Int = -intMinValue
val i5: Int = intMinValue - 1
val i6: Int = intMinValue - intMaxValue
val i7: Int = intMinValue + intMaxValue
val i8: Int = -intMaxValue
val i10: Int = intMinValue * -1
val i11: Int = intMinValue * 2
val i12: Int = intMaxValue * -2
val i13: Int = intMaxValue * -1
val i15: Int = intMinValue / -1
val l20: Int = 30 * 24 * 60 * 60 * 1000
val l21: Int = intMinValue - intMinValue
val l22: Int = intMinValue + -intMinValue
val l23: Int = intMaxValue + -intMinValue
val l25: Int = (-1).rem(5)
val l26: Int = (-1) % 5


fun foo() {
    val a3: Int = intMaxValue + 1 - 10
    val a4: Int = intMaxValue + 1 + 10
    val i2: Int = intMaxValue - 1 + 2
    val i3: Int = intMaxValue - intMinValue
    val i4: Int = -intMinValue
    val i5: Int = intMinValue - 1
    val i6: Int = intMinValue - intMaxValue
    val i7: Int = intMinValue + intMaxValue
    val i8: Int = -intMaxValue
    val i10: Int = intMinValue * -1
    val i11: Int = intMinValue * 2
    val i12: Int = intMaxValue * -2
    val i13: Int = intMaxValue * -1
    val i15: Int = intMinValue / -1
    val l20: Int = 30 * 24 * 60 * 60 * 1000
    val l21: Int = intMinValue - intMinValue
    val l22: Int = intMinValue + -intMinValue
    val l23: Int = intMaxValue + -intMinValue
    val l25: Int = (-1).rem(5)
    val l26: Int = (-1) % 5
}

class A {
    fun foo() {
        val a3: Int = intMaxValue + 1 - 10
        val a4: Int = intMaxValue + 1 + 10
        val i2: Int = intMaxValue - 1 + 2
        val i3: Int = intMaxValue - intMinValue
        val i4: Int = -intMinValue
        val i5: Int = intMinValue - 1
        val i6: Int = intMinValue - intMaxValue
        val i7: Int = intMinValue + intMaxValue
        val i8: Int = -intMaxValue
        val i10: Int = intMinValue * -1
        val i11: Int = intMinValue * 2
        val i12: Int = intMaxValue * -2
        val i13: Int = intMaxValue * -1
        val i15: Int = intMinValue / -1
        val l20: Int = 30 * 24 * 60 * 60 * 1000
        val l21: Int = intMinValue - intMinValue
        val l22: Int = intMinValue + -intMinValue
        val l23: Int = intMaxValue + -intMinValue
        val l25: Int = (-1).rem(5)
        val l26: Int = (-1) % 5
    }
}