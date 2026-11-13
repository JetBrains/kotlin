// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtProperty
class FooBar {
    val p: String

    constructor(p: String) {
        this.p = p
    }

    fun doSmth() {}
}
