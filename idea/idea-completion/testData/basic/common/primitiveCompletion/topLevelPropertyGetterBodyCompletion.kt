// FIR_COMPARISON

fun localFun(): Int {}

val property: Int
    get() {
        <caret>
    }

// EXIST: localFun