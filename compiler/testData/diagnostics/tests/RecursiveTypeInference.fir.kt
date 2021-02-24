// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

// FILE: a.kt
package a
    val foo = bar()

    fun bar() = foo

// FILE: b.kt
package b
    fun foo() = bar()

    fun bar() = foo()

// FILE: c.kt
package c
    fun bazz() = bar()

    fun foo() = bazz()

    fun bar() = foo()

// FILE: d.kt

    package ok.a
        val foo = bar()

        fun bar() : Int = foo

// FILE: e.kt
    package ok.b
        fun foo() : Int = bar()

        fun bar() = foo()

// FILE: f.kt
    package ok.c
        fun bazz() = bar()

        fun foo() : Int = bazz()

        fun bar() = foo()
