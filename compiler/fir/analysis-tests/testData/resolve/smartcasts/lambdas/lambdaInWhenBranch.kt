// ISSUE: KT-39080
// !DUMP_CFG

private sealed class Sealed

private data class SubClass1(val t: String) : Sealed()
private data <!PRIMARY_CONSTRUCTOR_REQUIRED_FOR_DATA_CLASS!>class SubClass2<!> : Sealed()

private fun foo(p: Sealed) {
    when (p) {
        is SubClass1 -> "".let {
            it
        }
        is SubClass2 -> ""
    }

    p.<!UNRESOLVED_REFERENCE!>t<!> // should not be resolved, but it has a smartcast to SubClass1 because of the lambda

    when (p) {
        is SubClass1 -> p.t
        is SubClass2 -> "2"
    }.length // should be resolved, but when is not considered as sealed because type of p is not a sealed class
}
