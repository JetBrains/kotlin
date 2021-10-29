// NI_EXPECTED_FILE

// FILE: a.kt
package a
    val foo = bar()

    fun bar() = <!DEBUG_INFO_MISSING_UNRESOLVED, TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!>foo<!>

// FILE: b.kt
package b
    fun foo() = bar()

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>()<!>

// FILE: c.kt
package c
    fun bazz() = bar()

    fun foo() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>bazz<!>()<!>

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
