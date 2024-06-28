// ISSUE: KT-59233

fun <T> consume(arg: T) {}

fun box(): String {
    val foo = ::<!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>consume<!>
    return "OK"
}
