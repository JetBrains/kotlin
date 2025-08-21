// JVM_DEFAULT_MODE: enable
// FILE: I.kt

interface I {
    fun foo(x: String = "OK"): String = x
}

// FILE: J.kt

interface J : I

// @I$DefaultImpls.class:
// 2 foo\$default

// @J$DefaultImpls.class:
// 0 foo\$default
