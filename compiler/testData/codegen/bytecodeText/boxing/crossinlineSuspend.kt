// !LANGUAGE: +ReleaseCoroutines

inline fun inlineMe(crossinline c: suspend () -> Int): suspend () -> Int {
    val i: suspend () -> Int = { c() + c() }
    return i
}

// 1 valueOf
// 0 boxInt
