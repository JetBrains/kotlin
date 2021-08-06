// !DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS
package f

fun <R> h(f: (Boolean) -> R) = 1
fun <R> h(f: (String) -> R) = 2

fun test() = <!OVERLOAD_RESOLUTION_AMBIGUITY!>h<!>{ i -> getAnswer() }

fun getAnswer() = 42
