// OUT_OF_CODE_BLOCK: TRUE
// SKIP_ANALYZE_CHECK
// ERROR: Function declaration must have a name

fun test() {<caret>

    val someThing: Any? = null
    val otherThing: Any? = null

    if (!(someThing?.equals(otherThing) ?: otherThing == null)) {
        // Some comment
    }

// TYPE: }