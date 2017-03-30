// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
package f

fun <R> h(<!UNUSED_PARAMETER!>f<!>: (Boolean) -> R) = 1
fun <R> h(<!UNUSED_PARAMETER!>f<!>: (String) -> R) = 2

fun test() = <!CANNOT_COMPLETE_RESOLVE!>h<!>{ <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>i<!> -> getAnswer() }

fun getAnswer() = 42

