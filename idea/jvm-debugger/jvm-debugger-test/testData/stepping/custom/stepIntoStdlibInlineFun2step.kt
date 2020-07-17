// FILE: stepIntoStdlibInlineFun2step.kt
package stepIntoStdlibInlineFun2step

fun main(args: Array<String>) {
    customLib.functionInLibrary.simpleFun()
}

// ADDITIONAL_BREAKPOINT: functionInLibrary.kt / public inline fun simpleFun()
// STEP_INTO: 5

// FILE: customLib/functionInLibrary/functionInLibrary.kt
package customLib.functionInLibrary

public inline fun simpleFun() {
    nextFun()
}

public inline fun nextFun() {
    val a = 1
}
