package dexInlineInObject

fun main(args: Array<String>) {
    dexInlineInObject.other.TestDexInlineInObject.inlineFun()
}

// ADDITIONAL_BREAKPOINT: dexInlineInObject.Other.kt: Breakpoint 1