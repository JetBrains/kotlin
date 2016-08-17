// "Create extension property 'A.foo'" "true"
// ERROR: Unresolved reference: foo

private val A.foo: String?<caret>

fun test(): String? {
    return A().foo
}