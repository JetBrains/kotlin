// RUN_PIPELINE_TILL: FRONTEND
const val c = <!CONST_VAL_WITH_NON_CONST_INITIALIZER!>1u + 2u<!>

fun box() = when {
    c != 3u -> "fail"
    else -> "OK"
}
