// IGNORE_BACKEND: JVM_IR
fun equals3(a: Byte?, b: Byte?) = a != null && b != null && a == b

fun equals4(a: Byte?, b: Byte?) = if (a is Byte && b is Byte) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Byte && b is Byte) a == b else null!!

fun less3(a: Byte?, b: Byte?) = a != null && b != null && a < b

fun less4(a: Byte?, b: Byte?) = if (a is Byte && b is Byte) a < b else true

fun less5(a: Any?, b: Any?) = if (a is Byte && b is Byte) a < b else true

// 3 Intrinsics\.areEqual
// 3 Intrinsics\.compare
// for compare:
// 3 IFGE
// 0 IF_ICMPGE
