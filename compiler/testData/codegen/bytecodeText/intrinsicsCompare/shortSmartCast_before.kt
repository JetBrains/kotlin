// LANGUAGE: -ProperIeee754Comparisons
// IGNORE_BACKEND: JVM_IR

fun equals3(a: Short?, b: Short?) = a != null && b != null && a == b

fun equals4(a: Short?, b: Short?) = if (a is Short && b is Short) a == b else null!!

fun equals5(a: Any?, b: Any?) = if (a is Short && b is Short) a == b else null!!

fun less3(a: Short?, b: Short?) = a != null && b != null && a < b

fun less4(a: Short?, b: Short?) = if (a is Short && b is Short) a < b else true

fun less5(a: Any?, b: Any?) = if (a is Short && b is Short) a < b else true

// JVM_TEMPLATES
// 3 Intrinsics\.areEqual
// 3 Intrinsics\.compare
// 3 IFGE
// 0 IF_ICMPGE

// JVM_IR_TEMPLATES
// 2 Intrinsics\.areEqual
// 0 Intrinsics\.compare
// 4 INVOKEVIRTUAL java/lang/Short\.shortValue \(\)S
// 4 INVOKEVIRTUAL java/lang/Number\.shortValue \(\)S
// 0 IFGE
// 3 IF_ICMPGE
// 0 IF_ICMPNE