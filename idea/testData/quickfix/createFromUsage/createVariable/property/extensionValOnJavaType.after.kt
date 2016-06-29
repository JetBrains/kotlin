// "Create extension property 'A.foo'" "true"
// ERROR: Unresolved reference: foo

val A.foo: String?

fun test(): String? {
    return A().foo
}