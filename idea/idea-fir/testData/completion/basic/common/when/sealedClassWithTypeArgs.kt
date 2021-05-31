sealed class SEALED
class AAAA<E, S>: SEALED()
object BBBB: SEALED()
class CCCC<E>: SEALED()

fun foo(e: SEALED) {
    when (e) {
        <caret>
    }
}

// EXIST: { lookupString: "is AAAA", tailText: "<*, *> -> " }
// EXIST: BBBB
// EXIST: { lookupString: "is CCCC", tailText: "<*> -> " }
// EXIST: { lookupString: "else -> "}
// NOTHING_ELSE
// FIR_COMPARISON
