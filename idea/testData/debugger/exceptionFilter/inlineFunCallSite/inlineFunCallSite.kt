fun box() {
    foo()
}

inline fun foo() {
    val unused = 1
    null!!
}

// NAVIGATE_TO_CALL_SITE
// FILE: inlineFunCallSite.kt
// LINE: 2
