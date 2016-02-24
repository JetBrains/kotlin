// FILE: A.kt

package library

public enum class EnumClass {
    ENTRY;

    public companion object {
        public fun entry(): EnumClass = ENTRY
    }
}

// FILE: B.kt

import library.EnumClass

fun main(args: Array<String>) {
    if (EnumClass.entry() != EnumClass.ENTRY) throw AssertionError()
}
