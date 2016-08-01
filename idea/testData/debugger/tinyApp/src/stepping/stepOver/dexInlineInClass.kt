package dexInlineInClass

fun main(args: Array<String>) {
    dexInlineInClass.other.TestDexInlineInClass().inlineFun()
}

// ADDITIONAL_BREAKPOINT: dexInlineInClass.Other.kt: Breakpoint 1