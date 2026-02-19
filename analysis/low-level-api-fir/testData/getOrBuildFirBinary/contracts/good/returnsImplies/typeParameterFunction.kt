// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtNamedFunction
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
public inline fun <reified T> assertIs(value: Any?): T {
    contract { returns() implies (value is T) }
    return value as T
}
