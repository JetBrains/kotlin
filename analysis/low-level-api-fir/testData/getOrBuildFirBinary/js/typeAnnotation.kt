// IGNORE_STANDALONE
// KT-91917
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references for annotated types
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty

val prop: @Y Int = 1

@Target(AnnotationTarget.TYPE)
annotation class Y
