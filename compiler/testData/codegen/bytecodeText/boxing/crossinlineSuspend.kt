// !LANGUAGE: +ReleaseCoroutines
// IGNORE_BACKEND: JVM_IR

inline fun inlineMe(crossinline c: suspend () -> Int): suspend () -> Int {
    val i: suspend () -> Int = { c() + c() }
    return i
}

// invokeSuspend$$forInline : valueOf
// invokeSuspend : boxInt

// 1 valueOf
// 1 boxInt
