import kotlin.contracts.*

@OptIn(kotlin.contracts.ExperimentalContracts::class)
inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

fun fooS(): String = ""

@IgnorableReturnValue
fun ign(): String = ""

fun testFunctions(s: String?) {
    s?.myLet { fooS() }
    s?.myLet { ign() }
}
