sealed class SEALED {
    class AAAA: SEALED()
    object BBBB: SEALED()
    class CCCC: SEALED()
}

fun foo(e: SEALED) {
    when (e) {
        <caret>
    }
}

// EXIST: is SEALED.AAAA
// EXIST: SEALED.BBBB
// EXIST: is SEALED.CCCC
// EXIST: { lookupString: "else -> "}
// NOTHING_ELSE
// FIR_COMPARISON
