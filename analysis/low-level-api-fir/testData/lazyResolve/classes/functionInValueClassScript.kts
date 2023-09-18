// SKIP_WHEN_OUT_OF_CONTENT_ROOT

@JvmInline
value class Value(val value: Int) {
    fun resolve<caret>Me() {}
}
