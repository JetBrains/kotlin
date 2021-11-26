// WITH_STDLIB
// FILE: 2.kt

fun box(): String = X(Z("OK")).z.result

// FILE: 1.kt

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val result: String)

class X(val z: Z)
