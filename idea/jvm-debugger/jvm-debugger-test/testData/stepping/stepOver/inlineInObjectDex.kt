package inlineInObjectDex

fun main(args: Array<String>) {
    inlineInObjectDex.other.TestDexInlineInObject.inlineFun()
}

// ADDITIONAL_BREAKPOINT: inlineInObjectDex.Other.kt: Breakpoint 1