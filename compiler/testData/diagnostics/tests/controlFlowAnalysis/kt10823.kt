fun find2(): Any? {
    fun visit(element: Any) {
        <!RETURN_NOT_ALLOWED!>return@find2<!> element
    }
    return null
}

// For find(): AssertionError at ControlFlowInstructionsGeneratorWorker.getExitPoint()

fun find(): Any? {
    object : Any() {
        fun visit(element: Any) {
            <!RETURN_NOT_ALLOWED!>return@find<!> element
        }
    }
    return null
}

fun find4(): Any? {
    <!NOT_YET_SUPPORTED_IN_INLINE!>inline<!> fun visit(element: Any) {
        <!RETURN_NOT_ALLOWED!>return@find4<!> element
    }
    return null
}

fun find3(): Any? {
    object : Any() {
        <!NOTHING_TO_INLINE!>inline<!> fun visit(element: Any) {
            <!RETURN_NOT_ALLOWED!>return@find3<!> element
        }
    }
    return null
}
