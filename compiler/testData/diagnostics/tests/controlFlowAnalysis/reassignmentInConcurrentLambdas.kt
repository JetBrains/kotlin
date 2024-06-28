// WITH_STDLIB
// ISSUE: KT-59669
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun <T> run2(x: () -> T, y: () -> T) {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
        callsInPlace(y, InvocationKind.EXACTLY_ONCE)
    }
    x()
    y()
}

fun test1() {
    val x: Int
    run2({ x = 1 }, { <!VAL_REASSIGNMENT!>x<!> = 2 })
    x.inc()
}

fun test2() {
    val x: Int
    run2({ x = 1 }, {})
    run2({}, { <!VAL_REASSIGNMENT!>x<!> = 2 })
    x.inc()
}

fun test3(p: Boolean) {
    val x: Int
    run2(
        {
            if (p) {
                x = 1
            } else {
                x = 2
            }
        },
        { <!VAL_REASSIGNMENT!>x<!> = 3 }
    )
    x.inc()
}

class C {
    val x: Int
    init {
        C().apply {
            run2({ <!VAL_REASSIGNMENT!>x<!> = 1 }, { <!VAL_REASSIGNMENT!>x<!> = 1 })
        }
        run2({ x = 1 }, { <!VAL_REASSIGNMENT!>x<!> = 2 })
        x.inc()
    }
}
