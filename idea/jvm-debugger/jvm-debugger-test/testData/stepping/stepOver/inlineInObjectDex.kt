// FILE: inlineInObjectDex.kt
// EMULATE_DEX: true

package inlineInObjectDex

fun main(args: Array<String>) {
    inlineInObjectDex.other.TestDexInlineInObject.inlineFun()
}

// ADDITIONAL_BREAKPOINT: inlineInObjectDex.Other.kt: Breakpoint 1

// FILE: inlineInObjectDex.Other.kt
package inlineInObjectDex.other

object TestDexInlineInObject {
    inline fun inlineFun() {
        // Breakpoint 1
        some()
        some()
    }

    fun some() {}
}