// STRING_CONCAT: indy-with-constants
// JVM_TARGET: 11

private fun getLong(): Long = 1

val leadingEmpty = "" + getLong()
val trailingEmpty = "" + getLong() + ""

// 0 LDC ""
// 0 INVOKEDYNAMIC makeConcatWithConstants
// 2 INVOKESTATIC java/lang/String.valueOf \(J\)
