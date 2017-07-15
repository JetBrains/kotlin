package stepIntoStdlibInlineFun2step

fun main(args: Array<String>) {
    customLib.functionInLibrary.simpleFun()
}

// ADDITIONAL_BREAKPOINT: functionInLibrary.kt:public inline fun simpleFun()
// STEP_INTO: 5