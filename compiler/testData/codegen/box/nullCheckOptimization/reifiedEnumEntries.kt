// ISSUE: KT-89861
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
import kotlin.enums.enumEntries

inline fun <reified T : Enum<T>> entriesIsList(): Boolean {
    val entries: Any = enumEntries<T>()
    return entries is List<*>
}

// MODULE: main(lib)
// FILE: main.kt
enum class E { A, B }

fun box(): String {
    return if (entriesIsList<E>()) "OK" else "Fail"
}
