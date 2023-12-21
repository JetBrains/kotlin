// ISSUE: KT-63414
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
inline fun test_1(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) } // should be ok
    try {
        f()
    } finally {

    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_2(f: () -> Int): Int {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) } // should be ok
    try {
        return f()
    } finally {
        println()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_3(f: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) }<!> // should be error
    try {
        f()
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_4(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) } // should be ok
    try {
        f()
    } catch (_: Exception) {
        throw Exception()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_5(f: () -> Unit) {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) } // should be ok
    try {
        f()
    } catch (_: Exception) {
        throw Exception()
    } finally {

    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_6(f: () -> Int): Int {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) } // should be ok
    try {
        return f()
    } catch (_: Exception) {
        throw Exception()
    } finally {

    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_7(f: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) }<!> // should be error
    try {
        f()
    } catch (_: Exception) {

    } finally {

    }
}

@OptIn(ExperimentalContracts::class)
inline fun test_8(f: () -> Int): Int {
    contract { callsInPlace(f, InvocationKind.EXACTLY_ONCE) } // should be error
    try {
        return f()
    } catch (_: Exception) {
        return 1
    } finally {

    }
}
