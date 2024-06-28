package test

interface Some {
    @Deprecated("some" + "message", ReplaceWith("some" + "replacement"), DeprecationLevel.WARNING)
    fun foo()
}
