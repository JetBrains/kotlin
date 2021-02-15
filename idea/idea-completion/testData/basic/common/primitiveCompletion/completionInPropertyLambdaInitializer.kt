// FIR_COMPARISON
val topLevelVal = ""

val otherVal: () -> Unit = {
    val foo = <caret>
}

// EXIST: topLevelVal
