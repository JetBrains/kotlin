// RUN_PIPELINE_TILL: FRONTEND
// API_VERSION: 1.5

import kotlin.time.Duration

@OptIn(kotlin.time.ExperimentalTime::class)
data class Some(val duration: Duration = Duration.INFINITE)

@OptIn(kotlin.time.ExperimentalTime::class)
fun foo(duration: Duration = Duration.INFINITE) {}

fun test() {
    <!OPT_IN_USAGE_FUTURE_ERROR("kotlin.time.ExperimentalTime; This declaration is experimental due to signature types and its usage must be marked (will become an error in future releases) with '@kotlin.time.ExperimentalTime' or '@OptIn(kotlin.time.ExperimentalTime::class)'")!>Some<!>()
    <!OPT_IN_USAGE_ERROR!>foo<!>()
}
