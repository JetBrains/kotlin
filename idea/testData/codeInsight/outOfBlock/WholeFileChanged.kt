// TRUE
// SKIP_ANALYZE_CHECK

fun test() {<caret>

    val someThing: Any? = null
    val otherThing: Any? = null

    if (!(someThing?.equals(otherThing) ?: otherThing == null)) {
        // Some comment
    }

// TYPE: }