// FILE: _.kt
package a
    val foo = bar()

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo<!>

// FILE: _.kt
package b
    fun foo() = bar()

    fun bar() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>foo()<!>

// FILE: _.kt
package c
    fun bazz() = bar()

    fun foo() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>bazz()<!>

    fun bar() = foo()

// FILE: _.kt

    package ok.a
        val foo = bar()

        fun bar() : Int = foo

// FILE: _.kt
    package ok.b
        fun foo() : Int = bar()

        fun bar() = foo()

// FILE: _.kt
    package ok.c
        fun bazz() = bar()

        fun foo() : Int = bazz()

        fun bar() = foo()
