@JvmInline
value class AnyWrapper(val payload: Any) {
    inline val describe
        get() = "bar_" + payload.toString()
}
