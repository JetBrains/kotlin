import kotlin.contracts.*

@Suppress(<!ERROR_SUPPRESSION!>"OPT_IN_USAGE_ERROR"<!>, "OPT_IN_USAGE_FUTURE_ERROR")
inline fun atLeastOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

@Suppress(<!ERROR_SUPPRESSION!>"OPT_IN_USAGE_ERROR"<!>, "OPT_IN_USAGE_FUTURE_ERROR")
inline fun atMostOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    block()
}

@Suppress(<!ERROR_SUPPRESSION!>"OPT_IN_USAGE_ERROR"<!>, "OPT_IN_USAGE_FUTURE_ERROR")
inline fun exactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun test() {
    var s: String? = null
    s = ""
    atLeastOnce {
        s<!UNSAFE_CALL!>.<!>length // unstable since lambda can be called twice
        s = null
        var s2: String? = null
        s2 = ""
        s2.length // local variable declared inside lambda is stable
        s2 = null
    }
    s = ""
    exactlyOnce {
        s.length // stable since lambda can be called only once
        s = null
        var s2: String? = null
        s2 = ""
        s2.length // local variable declared inside lambda is stable
        s2 = null
    }
    s = ""
    atMostOnce {
        s.length // stable since lambda can be called at most once
        s = null
        var s2: String? = null
        s2 = ""
        s2.length // local variable declared inside lambda is stable
        s2 = null
    }
}
