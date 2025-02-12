// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Result {
    class Success : Result()
    class Failure : Result()

    @OptIn(ExperimentalContracts::class)
    fun isFail<caret>ure(): Boolean {
        contract {
            returns() implies (this@Result is Failure)
        }

        return this is Failure
    }
}
