package smartStepIntoToLambdaParameter

fun nonDefaultParameter(paramFun: (Int) -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(1)
}

fun defaultParameter(f: (String) -> Int = { it.toInt() }) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    f("12")
}

fun badSignatureIsSkipped(f: (String) -> Int = { it.toInt() }, paramFun: (Int) -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(f("12"))
}

fun nonDefaultWithAnonymousFun(paramFun: (Int) -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    paramFun(12)
}

fun withExtensionParameters(paramFun: Int.() -> Int) {
    // SMART_STEP_INTO_BY_INDEX: 0
    // RESUME: 1
    //Breakpoint!
    12.paramFun()
}

fun main(args: Array<String>) {
    val localFun : (Int) -> Int = { it + 1 }

    nonDefaultParameter(localFun)

    defaultParameter()

    badSignatureIsSkipped { it + 1 }

    nonDefaultWithAnonymousFun(fun (i: Int): Int { return i })

    withExtensionParameters { this }
}