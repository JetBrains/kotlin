// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun check(value: Boolean) {
    contr<caret>act {
        returns() implies value
    }
    if (!value) {
        throw IllegalStateException()
    }
}
