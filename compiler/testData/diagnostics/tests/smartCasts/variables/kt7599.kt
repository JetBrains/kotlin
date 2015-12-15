interface A {
    fun ok(): Boolean
}

class B: A {
    override fun ok(): Boolean { return true }
}

class C: A {
    override fun ok(): Boolean { return false }
}

fun foo(): Boolean {
    var v: A
    v = B()
    // No smart cast needed, but not a problem if ever
    if (v.ok()) {
        v = C()
    }
    // No smart cast needed, and no smart cast possible!
    // We cannot choose between B and C
    return v.ok()
}
