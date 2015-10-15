fun x(): Boolean { return true }

public fun foo(p: String?): Int {
    // See KT-6283
    do {
        if (p != null) break
    } while (!x())
    // p can be null despite of the break
    return p<!UNSAFE_CALL!>.<!>length
}