fun box(): String {
    val lhs: Any? = true
    val rhs: Any? = false
    if (lhs is Boolean && rhs is Boolean) {
        if (lhs.compareTo(rhs) == 1) return "OK"
    }
    return "Fail"
}