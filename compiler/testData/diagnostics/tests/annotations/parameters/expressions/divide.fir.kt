// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

@Ann(<!ARGUMENT_TYPE_MISMATCH!>1 / 1<!>, <!ARGUMENT_TYPE_MISMATCH!>1 / 1<!>, 1 / 1, <!ARGUMENT_TYPE_MISMATCH!>1 / 1<!>) class MyClass

// EXPECTED: @Ann(b = 1.toByte(), i = 1, l = 1.toLong(), s = 1.toShort())
