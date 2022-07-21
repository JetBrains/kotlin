// LANGUAGE: -ApproximateIntegerLiteralTypesInReceiverPosition
// IGNORE_BACKEND_K2: JVM_IR
// For reasons this test is ignored, go to KT-46419

val a: Short = 1 + 255

// 1 SIPUSH 256
