private fun getLong(): Long = 1

private fun getString(): String = "s"

val leadingEmpty = "" + getLong()
val trailingEmpty = "" + getLong() + ""

fun plusString() = getString() + ""

fun plusNullableString(s: String?) = "" + s

fun middleEmpty() = getString() + "" + getString()

fun box(): String = "OK"

// 0 LDC ""
// 2 INVOKESTATIC java/lang/String.valueOf \(J\)
// 2 INVOKESTATIC java/lang/String.valueOf \(Ljava/lang/Object;\)
// 1 NEW java/lang/StringBuilder
// 4 valueOf
