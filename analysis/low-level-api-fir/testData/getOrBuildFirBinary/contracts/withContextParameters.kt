// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
@file:OptIn(ExperimentalContracts::class)
import kotlin.contracts.*

context(a: Boolean?)
fun conditionInContext() {
    contract { returns() implies (a != null) }
}
