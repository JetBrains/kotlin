// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: A.kt
private class Private {
    fun foo() = "OK"
}

internal inline fun internalInlineFun(): String {
    @Suppress("PRIVATE_CLASS_MEMBER_FROM_INLINE")
    return Private().foo()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
