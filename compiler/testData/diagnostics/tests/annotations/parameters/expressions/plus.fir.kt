// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

@Ann(<!ARGUMENT_TYPE_MISMATCH!>1 + 1<!>, <!ARGUMENT_TYPE_MISMATCH!>1 + 1<!>, 1 + 1, <!ARGUMENT_TYPE_MISMATCH!>1 + 1<!>) class MyClass

// EXPECTED: @Ann(b = 2.toByte(), i = 2, l = 2.toLong(), s = 2.toShort())
