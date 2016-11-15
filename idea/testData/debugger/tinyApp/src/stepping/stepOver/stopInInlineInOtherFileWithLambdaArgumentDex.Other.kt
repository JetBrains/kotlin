package stopInInlineInOtherFileWithLambdaArgumentDex

inline fun inlineFun(a: () -> Unit) {
    a()
    // Breakpoint 1
    a()
    a()
}