class ExplicitAccessorForAnnotation {
    val tt: String? = "good"
        get

    fun foo(): String {
        if (tt is String) {
            return <!SMARTCAST_IMPOSSIBLE!>tt<!>
        }
        return ""
    }
}