// TARGET_BACKEND: JVM_IR
// Reason: non-jvm tests don't support overriding declarations, and fail with: IrPropertySymbolImpl is already bound. Signature: /b|{}b[0].
// MODULE: lib
// FILE: 2.kt
val a get() = "OK"
val b get() = a

// FILE: 3.kt
val c get() = b

// MODULE: main(lib)
// FILE: 1.kt
val d get() = c

fun box(): String = d

// FILE: 2.kt
val a get() = "OK"
val b get() = a
