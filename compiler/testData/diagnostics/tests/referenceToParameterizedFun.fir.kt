// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59233

fun <T> consume(arg: T) {}

fun box(): String {
    val foo = ::<!CANNOT_INFER_PARAMETER_TYPE!>consume<!>
    return "OK"
}
