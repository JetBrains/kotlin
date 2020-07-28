import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun Any?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)
    }
    return this != null
}

@OptIn(ExperimentalContracts::class)
val Any?.isNotNull: Boolean
    get() {
        <!WRONG_IMPLIES_CONDITION!>contract {
            returns(true) implies (this@isNotNull != null)
        }<!>
        return this@isNotNull != null
    }