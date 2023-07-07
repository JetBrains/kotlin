// FIR_IDENTICAL
// ISSUE: KT-59233

fun <T> consume(arg: T) {}

fun box(): String {
    val foo = ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>consume<!>
    return "OK"
}
