// NI_EXPECTED_FILE

// FILE: a.kt
package a
    val foo = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo<!>

// FILE: b.kt
package b
    fun foo() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo()<!>

// FILE: c.kt
package c
    fun bazz() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bar()<!>

    fun foo() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bazz()<!>

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo()<!>

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
