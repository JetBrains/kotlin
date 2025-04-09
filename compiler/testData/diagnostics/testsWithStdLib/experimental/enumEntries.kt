// RUN_PIPELINE_TILL: FRONTEND
// API_VERSION: 1.8

import kotlin.enums.EnumEntries

enum class E

@OptIn(kotlin.ExperimentalStdlibApi::class)
data class Some(val values: EnumEntries<E> = E.entries)

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun foo(values: EnumEntries<E> = E.entries) {}

fun test() {
    <!OPT_IN_USAGE_FUTURE_ERROR("kotlin.ExperimentalStdlibApi; This declaration is experimental due to signature types and its usage must be marked (will become an error in future releases) with '@kotlin.ExperimentalStdlibApi' or '@OptIn(kotlin.ExperimentalStdlibApi::class)'")!>Some<!>()
    <!OPT_IN_USAGE_ERROR!>foo<!>()
}
