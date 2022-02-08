// WITH_STDLIB
// USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi

import kotlin.reflect.typeOf

fun <T : Comparable<T>> foo() {
    <!TYPEOF_NON_REIFIED_TYPE_PARAMETER_WITH_RECURSIVE_BOUND!>typeOf<List<T>>()<!>
}
