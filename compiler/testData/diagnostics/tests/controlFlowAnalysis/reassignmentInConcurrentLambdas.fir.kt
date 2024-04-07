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
    run2({ <!CAPTURED_VAL_INITIALIZATION!>x<!> = 1 }, { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 2 })
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
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = 1
            } else {
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = 2
            }
        },
        { <!CAPTURED_VAL_INITIALIZATION!>x<!> = 3 }
    )
    x.inc()
}

class C {
    val x: Int
    init {
        C().apply {
            run2({ <!VAL_REASSIGNMENT!>x<!> = 1 }, { <!VAL_REASSIGNMENT!>x<!> = 1 })
        }
        run2({ <!CAPTURED_MEMBER_VAL_INITIALIZATION!>x<!> = 1 }, { <!CAPTURED_MEMBER_VAL_INITIALIZATION!>x<!> = 2 })
        x.inc()
    }
}
