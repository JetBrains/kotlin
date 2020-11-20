// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

// FILE: b.kt
<!HIDDEN!>@file:JvmPackageName("")<!>
package b
fun b() {}

// FILE: c.kt
<!HIDDEN!>@file:JvmPackageName("invalid-fq-name")<!>
package c
fun c() {}

// FILE: d.kt
<!HIDDEN!>@file:JvmPackageName("d")<!>
package d
class D
fun d() {}

// FILE: e.kt
<!HIDDEN!>@file:JvmPackageName(42)<!>
package e
fun e() {}

// FILE: f.kt
<!HIDDEN!>@file:JvmPackageName(f)<!>
package f
const val name = "f"
