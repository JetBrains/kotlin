enum class ENUM {
    AAAA, BBBB, CCCC
}

fun foo(e: ENUM) {
    when (e) {
        ENUM.AAAA, ENUM.CCCC -> TODO()
        <caret>
    }
}

// EXIST: ENUM.BBBB
// EXIST: { lookupString: "else -> "}
// NOTHING_ELSE
// FIR_COMPARISON
