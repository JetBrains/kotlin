// LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
// IGNORE_BACKEND_FIR: JVM_IR
// For reasons this test is ignored, go to KT-46419

val a: Byte = 1 + 10

// 1 BIPUSH 11
