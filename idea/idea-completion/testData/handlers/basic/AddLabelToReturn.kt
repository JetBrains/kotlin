// FIR_COMPARISON
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

val v: Boolean = run {
    return<caret> true
}

// ELEMENT: "return@run"