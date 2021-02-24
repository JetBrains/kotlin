interface I {
    val p: Int
}

class CCCC(
        over<caret>
    private val v1: Int,
    private val v2: Int
) : I

// ELEMENT_TEXT: "override val p: Int"
