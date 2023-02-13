// MODULE: lib
// FILE: A.kt

package library

public enum class EnumClass {
    ENTRY;

    public companion object {
        public fun entry(): EnumClass = ENTRY
    }
}

// MODULE: main(lib)
// FILE: B.kt

import library.EnumClass

fun box(): String {
    return if (EnumClass.entry() != EnumClass.ENTRY) "Fail" else "OK"
}
