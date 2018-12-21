// IGNORE_BACKEND: JVM_IR
fun equals3(a: Char?, b: Char?) = a != null && b != null && a == b

fun equals4(a: Char?, b: Char?) = if (a is Char && b is Char) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Char && b is Char) a == b else null!!

fun less3(a: Char?, b: Char?) = a != null && b != null && a < b

fun less4(a: Char?, b: Char?) = if (a is Char && b is Char) a < b else true

fun less5(a: Any?, b: Any?) = if (a is Char && b is Char) a < b else true

// 3 Intrinsics\.areEqual
// 3 Intrinsics\.compare
// for compare:
// 3 IFGE
// 0 IF_ICMPGE
