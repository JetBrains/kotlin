// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

// FILE: b.kt
@file:JvmPackageName("")
package b
fun b() {}

// FILE: c.kt
@file:JvmPackageName("invalid-fq-name")
package c
fun c() {}

// FILE: d.kt
@file:JvmPackageName("d")
package d
class D
fun d() {}

// FILE: e.kt
@file:JvmPackageName(42)
package e
fun e() {}

// FILE: f.kt
@file:JvmPackageName(f)
package f
const val name = "f"
