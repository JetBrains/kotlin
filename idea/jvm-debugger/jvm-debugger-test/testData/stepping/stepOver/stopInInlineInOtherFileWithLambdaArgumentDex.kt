// FILE: stopInInlineInOtherFileWithLambdaArgumentDex.kt
// EMULATE_DEX: true

package stopInInlineInOtherFileWithLambdaArgumentDex

fun main(args: Array<String>) {
    inlineFun { "hi" }
    val i = 1
}

// ADDITIONAL_BREAKPOINT: stopInInlineInOtherFileWithLambdaArgumentDex.Other.kt: Breakpoint 1

// FILE: stopInInlineInOtherFileWithLambdaArgumentDex.Other.kt
package stopInInlineInOtherFileWithLambdaArgumentDex

inline fun inlineFun(a: () -> Unit) {
    a()
    // Breakpoint 1
    a()
    a()
}