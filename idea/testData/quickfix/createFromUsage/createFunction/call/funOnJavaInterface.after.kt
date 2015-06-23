// "Create member function 'foo'" "true"
// ERROR: Unresolved reference: foo

fun test(a: A): Int? {
    return a.foo<String, Int>(1, "2")
}