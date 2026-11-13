// IGNORE_STANDALONE
// `LLStandaloneFirElementByPsiElementChooser` crashes on building FIR type references with `createEmptySession()`
// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
@file:OptIn(ExperimentalContracts::class)
import kotlin.contracts.*

context(a: Boolean?)
fun conditionInContext() {
    contract { returns() implies (a != null) }
}
