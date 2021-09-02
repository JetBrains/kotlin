import kotlin.contracts.*

@Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE_FUTURE_ERROR")
inline fun atLeastOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

@Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE_FUTURE_ERROR")
inline fun atMostOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    block()
}

@Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE_FUTURE_ERROR")
inline fun exactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun test() {
    var s: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
    s = ""
    atLeastOnce {
        <!SMARTCAST_IMPOSSIBLE!>s<!>.length // unstable since lambda can be called twice
        s = null
        var s2: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
        s2 = ""
        <!DEBUG_INFO_SMARTCAST!>s2<!>.length // local variable declared inside lambda is stable
        s2 = null
    }
    s = ""
    exactlyOnce {
        <!SMARTCAST_IMPOSSIBLE!>s<!>.length // stable since lambda can be called only once
        s = null
        var s2: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
        s2 = ""
        <!SMARTCAST_IMPOSSIBLE!>s2<!>.length // local variable declared inside lambda is stable
        s2 = null
    }
    s = ""
    atMostOnce {
        <!SMARTCAST_IMPOSSIBLE!>s<!>.length // stable since lambda can be called at most once
        s = null
        var s2: String? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>null<!>
        s2 = ""
        <!SMARTCAST_IMPOSSIBLE!>s2<!>.length // local variable declared inside lambda is stable
        s2 = null
    }
}
