import kotlin.contracts.*

interface A

fun A.foo() {}

@OptIn(ExperimentalContracts::class)
fun Any?.myRequireNotNull() {
    contract {
        returns() implies (this@myRequireNotNull != null)
    }
    if (this == null) throw IllegalStateException()
}
