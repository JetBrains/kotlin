// IGNORE_BACKEND_K2: JVM_IR
// FIR status: KT-46419, ILT conversions to Byte and Short are not supported by design

val a: Short = 1 + 255

// 1 SIPUSH 256
