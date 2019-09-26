package stopInInlineInOtherFileWithLambdaArgumentDex

fun main(args: Array<String>) {
    inlineFun { "hi" }
    val i = 1
}

// ADDITIONAL_BREAKPOINT: stopInInlineInOtherFileWithLambdaArgumentDex.Other.kt: Breakpoint 1