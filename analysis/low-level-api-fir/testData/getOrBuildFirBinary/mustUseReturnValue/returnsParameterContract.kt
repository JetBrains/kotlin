// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=full
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction

import kotlin.contracts.*

@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T> T.myApply(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsParameter(this@myApply)
    }
    block()
    return this
}
