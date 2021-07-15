// !LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

@Ann(<!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>, <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>, 1 - 1, <!INTEGER_OPERATOR_RESOLVE_WILL_CHANGE!>1 - 1<!>) class MyClass

// EXPECTED: @Ann(b = 0.toByte(), i = 0, l = 0.toLong(), s = 0.toShort())
