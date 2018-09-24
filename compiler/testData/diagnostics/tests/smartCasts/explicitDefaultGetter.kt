// !WITH_NEW_INFERENCE
class ExplicitAccessorForAnnotation {
    val tt: String? = "good"
        get

    fun foo(): String {
        if (tt is String) {
            return <!NI;TYPE_MISMATCH, SMARTCAST_IMPOSSIBLE!>tt<!>
        }
        return ""
    }
}