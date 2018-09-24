// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

// FILE: f.kt
package a
    val foo = bar()

    fun bar() = <!NI;DEBUG_INFO_MISSING_UNRESOLVED, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo<!>

// FILE: f.kt
package b
    fun foo() = bar()

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()<!>

// FILE: f.kt
package c
    fun bazz() = bar()

    fun foo() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED!>bazz<!>()<!>

    fun bar() = foo()

// FILE: f.kt

    package ok.a
        val foo = bar()

        fun bar() : Int = foo

// FILE: f.kt
    package ok.b
        fun foo() : Int = bar()

        fun bar() = foo()

// FILE: f.kt
    package ok.c
        fun bazz() = bar()

        fun foo() : Int = bazz()

        fun bar() = foo()
