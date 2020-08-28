class A {
    operator fun invoke() = Unit

    fun a() {
        this(<caret>)
    }
}

// SET_FALSE: ALIGN_MULTILINE_METHOD_BRACKETS