fun find2(): Any? {
    fun visit(element: Any) {
        return@find2 element
    }
    return null
}

// For find(): AssertionError at ControlFlowInstructionsGeneratorWorker.getExitPoint()

fun find(): Any? {
    object : Any() {
        fun visit(element: Any) {
            return@find element
        }
    }
    return null
}

fun find4(): Any? {
    inline fun visit(element: Any) {
        return@find4 element
    }
    return null
}

fun find3(): Any? {
    object : Any() {
        inline fun visit(element: Any) {
            return@find3 element
        }
    }
    return null
}
