import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface Result<out ValueType, out ErrorType> {
    data class Ok<ValueType>(val value: ValueType) : Result<ValueType, Nothing>
    data class Error<ErrorType>(val error: ErrorType) : Result<Nothing, ErrorType>
}

@OptIn(ExperimentalContracts::class)
fun <ValueType, ErrorType> Result<ValueType, ErrorType>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is Result.Error<ErrorType>)
        returns(false) implies (this@isError is Result.Ok<ValueType>)
    }
    return this is Result.Error
}
