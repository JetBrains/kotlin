// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

val prop: @Y Int = 1

@Target(AnnotationTarget.TYPE)
annotation class Y
