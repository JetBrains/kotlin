// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
val foo get() = "str"
