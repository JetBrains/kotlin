fun String.next(): String {
    return "abc"
}

fun list(start: String) {
    var e: Any? = start
    if (e==null) return
    while (e is String) {
        // Smart cast due to the loop condition
        if (e.length == 0)
            break
        // We still have smart cast here despite of a break
        e = e.next()
    }
    // e can never be null but we do not know it
    e<!UNSAFE_CALL!>.<!>hashCode()
}
