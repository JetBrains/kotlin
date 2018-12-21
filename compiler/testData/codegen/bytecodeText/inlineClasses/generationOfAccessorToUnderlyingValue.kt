// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class UInt(val value: Int)

// 0 INVOKESTATIC UInt\$Erased.getValue

// 0 valueOf
// 0 intValue