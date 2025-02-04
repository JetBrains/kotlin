// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_PARTIAL_BODY_ANALYSIS
import kotlin.contracts.*

@Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE_FUTURE_ERROR")
inline fun atLeastOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
}

fun test() {
    var s: String? = null
    s = ""
    atLeastOnce {
        s<!UNSAFE_CALL!>.<!>length // unstable since lambda can be called twice
    }
}
