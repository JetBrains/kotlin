// FIR_COMPARISON
val topLevelVal = ""

fun topLevel(topLevelArg: String) {
    val inTopLevelVal = 10

    fun local(localArg: String) {
        val inLocalVal = 20
        <caret>
    }
}

// EXIST: topLevelVal
// EXIST: topLevelArg
// EXIST: inTopLevelVal
// EXIST: localArg
// EXIST: inLocalVal