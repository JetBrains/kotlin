public fun test(o: String?): Boolean {
    return when {
        // Data flow info should propagate from o == null to o.length
        o == null, o.length == 0 -> false
        else -> true
    }
}