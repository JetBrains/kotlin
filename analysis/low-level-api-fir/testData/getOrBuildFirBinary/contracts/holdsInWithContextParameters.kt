// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// LANGUAGE: +HoldsInContracts, +ContextParameters
@file:OptIn(ExperimentalContracts::class, ExperimentalExtendedContracts::class)
import kotlin.contracts.*

class Foo {
    context(a: Boolean)
    inline fun conditionInContext(block: () -> Int) {
        contract { a holdsIn block }
        block()
    }
}
