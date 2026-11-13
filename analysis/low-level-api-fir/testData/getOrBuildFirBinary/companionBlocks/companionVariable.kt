// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
// MAIN_FILE_NAME: CompanionVariableKt

class Foo

companion var Foo.static1: Int
    get() = 1
    set(value) {

    }
