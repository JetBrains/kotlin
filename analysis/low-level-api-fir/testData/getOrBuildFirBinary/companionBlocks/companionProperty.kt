// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
// MAIN_FILE_NAME: CompanionPropertyKt

class Foo

companion val Foo.static1: Int get() = 1
