class Delegate {
    fun get(thisRef: Any?, prop: PropertyMetadata): String? {
        return null
    }
}

class Example {
    private val p: String? by Delegate()

    public val r: String? = "xyz"

    public fun foo(): String {
        // Smart cast is not possible if property is delegated
        return if (p != null) <!TYPE_MISMATCH!>p<!> else ""
    }

    public fun bar(): String {
        // But is possible for non-delegated value property even if it's public
        return if (r != null) <!DEBUG_INFO_SMARTCAST!>r<!> else ""
    }
}

