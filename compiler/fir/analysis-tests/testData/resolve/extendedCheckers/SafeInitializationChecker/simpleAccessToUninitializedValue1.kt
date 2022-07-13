// WITH_STDLIB

class A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val b: String = foo()<!>
    fun foo() = b.substring(1)
}

class B(bb: B, val bool: Boolean) {
    fun foo(b: B): Int =
        if (bool) d.hashCode() else 1

    <!ACCESS_TO_UNINITIALIZED_VALUE!>val d = foo(<!VALUE_CANNOT_BE_PROMOTED!>this<!>)<!>
    val c = foo(bb)
    val hello = Hello()
}

class Hello {
    fun foo(): String = name

    val nameLength = foo().length
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val name = "Alice"<!>
}

