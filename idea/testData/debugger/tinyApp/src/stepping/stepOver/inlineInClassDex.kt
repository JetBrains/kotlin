package inlineInClassDex

fun main(args: Array<String>) {
    inlineInClassDex.other.TestDexInlineInClass().inlineFun()
}

// ADDITIONAL_BREAKPOINT: inlineInClassDex.Other.kt: Breakpoint 1