// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
package test

annotation class Ann(<!MISSING_VAL_ON_ANNOTATION_PARAMETER!>p1: Int<!>,
                     <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>p2: Short<!>,
                     <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>p3: Byte<!>,
                     <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>p4: Int<!>,
                     <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>p5: Int<!>,
                     <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>p6: Int<!>
                     )

@Ann(1 or 1, <!ARGUMENT_TYPE_MISMATCH!>1 and 1<!>, <!ARGUMENT_TYPE_MISMATCH!>1 xor 1<!>, 1 shl 1, 1 shr 1, 1 ushr 1) class MyClass

// EXPECTED: @Ann(p1 = 1, p2 = 1.toShort(), p3 = 0.toByte(), p4 = 2, p5 = 0, p6 = 0)
