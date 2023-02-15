// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +ImplicitSignedToUnsignedIntegerConversion

const val IMPLICIT_INT = 255
const val EXPLICIT_INT: Int = 255
const val LONG_CONST = 255L
val NON_CONST = 255
const val BIGGER_THAN_UBYTE = 256
const val UINT_CONST = 42u

fun takeUByte(u: UByte) {}
fun takeUShort(u: UShort) {}
fun takeUInt(u: UInt) {}
fun takeULong(u: ULong) {}

fun takeUBytes(vararg u: UByte) {}

fun takeLong(l: Long) {}

fun takeUIntWithoutAnnotaion(u: UInt) {}

fun takeIntWithoutAnnotation(i: Int) {}

fun test() {
    takeUByte(IMPLICIT_INT)
    takeUByte(EXPLICIT_INT)

    takeUShort(IMPLICIT_INT)
    takeUShort(BIGGER_THAN_UBYTE)

    takeUInt(IMPLICIT_INT)

    takeULong(IMPLICIT_INT)

    takeUBytes(IMPLICIT_INT, EXPLICIT_INT, 42u)

    takeLong(<!TYPE_MISMATCH!>IMPLICIT_INT<!>)

    takeIntWithoutAnnotation(IMPLICIT_INT)

    takeUIntWithoutAnnotaion(UINT_CONST)

    takeUByte(<!TYPE_MISMATCH!>LONG_CONST<!>)
    takeUByte(<!TYPE_MISMATCH!>NON_CONST<!>)
    takeUByte(<!TYPE_MISMATCH!>BIGGER_THAN_UBYTE<!>)
    takeUByte(<!TYPE_MISMATCH!>UINT_CONST<!>)
    takeUIntWithoutAnnotaion(IMPLICIT_INT)
}
