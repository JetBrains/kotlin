// "Create extension property 'A.foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): String? {
    return A().foo
}

private val A.foo: String?
    get() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
