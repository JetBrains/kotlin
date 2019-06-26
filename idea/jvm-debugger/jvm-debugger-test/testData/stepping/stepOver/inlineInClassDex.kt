// FILE: inlineInClassDex.kt
// EMULATE_DEX: true

package inlineInClassDex

fun main(args: Array<String>) {
    inlineInClassDex.other.TestDexInlineInClass().inlineFun()
}

// ADDITIONAL_BREAKPOINT: inlineInClassDex.Other.kt: Breakpoint 1

// FILE: inlineInClassDex.Other.kt
package inlineInClassDex.other

class TestDexInlineInClass {
    inline fun inlineFun() {
        // Breakpoint 1
        some()
        some()
    }

    fun some() {}
}
