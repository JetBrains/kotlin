// ISSUE: KT-50875

interface A {
    fun foo()
}

@Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
fun <K> checkNotNull(x: K?, y: K): @kotlin.internal.Exact K {
    return x ?: y
}

fun test_1(any: Any, a: A, nullableA: A?) {
    var x = any // x has type Any

    x = checkNotNull(nullableA, a) // inferred to Any
    x.<!UNRESOLVED_REFERENCE!>foo<!>() // no smartcast

    x = nullableA ?: a // inferred to Any
    x.<!UNRESOLVED_REFERENCE!>foo<!>() // no smartcast

    x = nullableA ?: return // inferred to Any, but has A type
    x.foo() // smartcast
}
