class Test {
    fun a(
        action: () -> Unit = fun(
            <caret>
        ))
}


// SET_FALSE: ALIGN_MULTILINE_METHOD_BRACKETS