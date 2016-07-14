// "Create member function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

internal fun test(a: A): Int? {
    return a.<caret>foo<String, Int>(1, "2")
}