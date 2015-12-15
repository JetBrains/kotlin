fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // Exotic variant with unused literal
    do <!UNUSED_LAMBDA_EXPRESSION!>{ ->
        p!!.length
    }<!> while (!x())
    // Literal is not called so p.length is unsafe
    return p<!UNSAFE_CALL!>.<!>length
}