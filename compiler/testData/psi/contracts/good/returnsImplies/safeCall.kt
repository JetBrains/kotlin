import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Int? {
    contract {
        returnsNotNull() implies (x != null)
    }

    return x?.length
}

@OptIn(ExperimentalContracts::class)
@Suppress("IMPOSSIBLE_IS_CHECK_ERROR")
fun test2(x: String?): Int? {
    contract {
        returnsNotNull() implies (x is Boolean)
    }

    return x?.length
}
