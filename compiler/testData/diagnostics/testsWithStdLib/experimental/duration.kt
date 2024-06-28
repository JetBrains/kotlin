// API_VERSION: 1.5

import kotlin.time.Duration

@OptIn(kotlin.time.ExperimentalTime::class)
data class Some(val duration: Duration = Duration.INFINITE)

@OptIn(kotlin.time.ExperimentalTime::class)
fun foo(duration: Duration = Duration.INFINITE) {}

fun test() {
    <!OPT_IN_USAGE_FUTURE_ERROR!>Some<!>()
    <!OPT_IN_USAGE_ERROR!>foo<!>()
}
