class NON_SEALED
class AAAA<E, S>: NON_SEALED()
object BBBB: NON_SEALED()
class CCCC<E>: NON_SEALED()

fun foo(e: NON_SEALED) {
    when (e) {
        <caret>
    }
}

// EXIST: { lookupString: "is AAAA", tailText: "<*, *> -> " }
// EXIST: BBBB
// EXIST: { lookupString: "is CCCC", tailText: "<*> -> " }
// EXIST: { lookupString: "else -> "}
// FIR_COMPARISON
