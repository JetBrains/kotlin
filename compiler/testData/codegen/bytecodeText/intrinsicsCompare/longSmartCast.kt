// IGNORE_BACKEND: JVM_IR
fun equals3(a: Long?, b: Long?) = a != null && b != null && a == b

fun equals4(a: Long?, b: Long?) = if (a is Long && b is Long) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Long && b is Long) a == b else null!!

fun less3(a: Long?, b: Long?) = a != null && b != null && a < b

fun less4(a: Long?, b: Long?) = if (a is Long && b is Long) a < b else true

fun less5(a: Any?, b: Any?) = if (a is Long && b is Long) a < b else true

// 3 Intrinsics\.areEqual
// 0 Intrinsics\.compare
// 3 LCMP
// for compare:
// 3 IFGE
// 0 IF_ICMPGE
