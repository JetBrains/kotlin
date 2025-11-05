// LANGUAGE: +ContextParameters
@file:OptIn(ExperimentalContracts::class)
import kotlin.contracts.*

context(a: Boolean?)
fun conditionInContext() {
    contract { returns() implies (a != null) }
}
