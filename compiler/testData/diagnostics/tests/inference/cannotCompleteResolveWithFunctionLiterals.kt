// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
package f

fun h<R>(<!UNUSED_PARAMETER!>f<!>: (Boolean) -> R) = 1
fun h<R>(<!UNUSED_PARAMETER!>f<!>: (String) -> R) = 2

fun test() = <!CANNOT_COMPLETE_RESOLVE!>h<!>{ <!CANNOT_INFER_PARAMETER_TYPE!>i<!> -> getAnswer() }

fun getAnswer() = 42

