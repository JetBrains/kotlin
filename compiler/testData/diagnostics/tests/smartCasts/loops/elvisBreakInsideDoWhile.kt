public fun foo(x: String?): Int {
    do {
        // After the check, smart cast should work
        x ?: break
        // x is not null in both branches
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } while (true)
    // x is null because of the break
    return x<!UNSAFE_CALL!>.<!>length
}