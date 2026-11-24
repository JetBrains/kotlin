// TARGET_BACKEND: WASM
// ISSUE: KT-91749
// MODULE: lib
// FILE: lib.kt
package defaults

fun combineDefaults(x: Int = 40, y: Long = x.toLong() + 2L): Long = x + y
@Suppress("NOTHING_TO_INLINE")
inline fun throughInline(x: Int = 0): Long = combineDefaults(x)

// MODULE: main(lib)
// FILE: main.kt
import defaults.*

fun box(): String {
    if (combineDefaults() != 82L) return "FAIL omitted"
    if (combineDefaults(0) != 2L) return "FAIL partial"
    if (throughInline() != 2L) return "FAIL inline omitted"
    if (throughInline(3) != 8L) return "FAIL inline explicit"
    if (combineDefaults(0, 0L) != 0L) return "FAIL explicit"
    if (throughInline(3) != combineDefaults(3)) return "FAIL direct control"
    return "OK"
}
