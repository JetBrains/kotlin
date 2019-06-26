// FILE: stopInInlineInOtherFileDex.kt
// EMULATE_DEX: true

package stopInInlineInOtherFileDex

fun main(args: Array<String>) {
    inlineFun()
}

// ADDITIONAL_BREAKPOINT: stopInInlineInOtherFileDex.Other.kt: Breakpoint 1

// FILE: stopInInlineInOtherFileDex.Other.kt
package stopInInlineInOtherFileDex

inline fun inlineFun() {
    var i = 1
    // Breakpoint 1
    i++
    i++
}