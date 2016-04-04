package smartStepIntoInsideLambda

fun main(args: Array<String>) {
    // SMART_STEP_INTO_BY_INDEX: 1
    //Breakpoint! (lambdaOrdinal = 1)
    foo { bar() }
}

fun foo(f: () -> Unit) {
    f()
}

fun bar() {
    val a = 1
}

