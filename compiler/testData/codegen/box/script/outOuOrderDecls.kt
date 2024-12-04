// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// JVM_ABI_K1_K2_DIFF: KT-63963, KT-63960
// FILE: test.kt

fun box(): String = "OK"

// FILE: script.kts

class A : B, C {}

interface B

interface C
