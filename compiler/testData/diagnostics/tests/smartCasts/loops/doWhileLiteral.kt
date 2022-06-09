// FIR_IDENTICAL
fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // Exotic variant with unused literal
    do { ->
        p!!.length
    } while (!x())
    // Literal is not called so p.length is unsafe
    return p<!UNSAFE_CALL!>.<!>length
}
