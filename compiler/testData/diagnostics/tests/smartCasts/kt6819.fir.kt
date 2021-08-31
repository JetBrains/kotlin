public fun test(o: String?): Boolean {
    return when {
        // Data flow info should propagate from o == null to o.length
        o == null<!COMMA_IN_WHEN_CONDITION_WITHOUT_ARGUMENT!>,<!> o.length == 0 -> false
        else -> true
    }
}
