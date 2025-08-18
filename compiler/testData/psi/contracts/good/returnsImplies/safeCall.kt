import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun test1(x: String?): Int? {
    contract {
        returnsNotNull() implies (x != null)
    }

    return x?.length
}

@OptIn(ExperimentalContracts::class)
fun test2(x: String?): Int? {
    contract {
        returnsNotNull() implies (x is String)
    }

    return x?.length
}
