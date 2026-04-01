// TARGET_BACKEND: JVM
// FILE: test.kt

fun box(): String = "OK"

// FILE: script.kts

class A : B, C {}

interface B

interface C
