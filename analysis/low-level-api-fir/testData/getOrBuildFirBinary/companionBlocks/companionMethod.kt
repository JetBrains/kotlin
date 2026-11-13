// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
// MAIN_FILE_NAME: CompanionMethodKt

class Foo

companion fun Foo.static1() {}
