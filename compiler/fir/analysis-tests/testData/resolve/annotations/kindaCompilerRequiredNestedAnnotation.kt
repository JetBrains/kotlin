// ISSUE: KT-61388
// FILE: a.kt
@SinceKotlin
class Some

// FILE: b.kt
@SinceKotlin.SinceKotlin
class Other

// FILE: SinceKotlin.kt
annotation class SinceKotlin {
    annotation class SinceKotlin
}
