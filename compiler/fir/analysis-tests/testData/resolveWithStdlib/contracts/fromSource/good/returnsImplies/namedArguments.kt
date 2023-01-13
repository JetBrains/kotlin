import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun foo(x: Any, y: Any) {
    contract {
        returns() implies (x is Int && y is String)
    }
    if (x !is Int || y !is String) {
        throw IllegalStateException()
    }
}

fun test_1(x: Any, y: Any) {
    foo(x = x, y = y)
    x.inc()
    y.length
}

fun test_2(x: Any, y: Any) {
    foo(x, y = y)
    x.inc()
    y.length
}

fun test_3(x: Any, y: Any) {
    foo(y = y, x = x)
    x.inc()
    y.length
}

