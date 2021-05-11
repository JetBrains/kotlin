// FIR_COMPARISON
seal<caret> fun interface A {
    fun aFunction()
}

// EXIST: "sealed"
// NOTHING_ELSE