// FILE: a.kt

fun foo() = 42

// FILE: b/c.kt

fun bar() = foo()

class C

// FILE: c/d/e.kt

fun qux(c: C) = c.toString()

fun baz() = qux(C()) + bar()