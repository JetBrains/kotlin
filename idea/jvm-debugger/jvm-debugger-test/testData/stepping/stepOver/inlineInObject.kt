// FILE: inlineInObject.kt
package inlineInObject

fun main(args: Array<String>) {
    inlineInObject.other.TestInlineInObject.inlineFun()
}

// ADDITIONAL_BREAKPOINT: inlineInObject.Other.kt: Breakpoint 1

// FILE: inlineInObject.Other.kt
package inlineInObject.other

object TestInlineInObject {
    inline fun inlineFun() {
        // Breakpoint 1
        some()
        some()
    }

    fun some() {}
}