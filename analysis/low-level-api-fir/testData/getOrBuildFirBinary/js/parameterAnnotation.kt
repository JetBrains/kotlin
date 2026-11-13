// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtFunction

fun fn(@Y p: String) {
}

annotation class Y
