// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
package f

fun <R> h(f: (Boolean) -> R) = 1
fun <R> h(f: (String) -> R) = 2

fun test() = <!CANNOT_COMPLETE_RESOLVE{OI}, OVERLOAD_RESOLUTION_AMBIGUITY{NI}!>h<!>{ <!CANNOT_INFER_PARAMETER_TYPE{OI}!>i<!> -> getAnswer() }

fun getAnswer() = 42
