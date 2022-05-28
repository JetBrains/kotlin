// WITH_STDLIB

class Small {
    fun foo() = b.substring(1)
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val b: String = foo()<!>
}

class B(bb: B) {
    fun foo(b: B): String =
        if (d.isEmpty()) d.substring(1) else ""

    <!ACCESS_TO_UNINITIALIZED_VALUE, ACCESS_TO_UNINITIALIZED_VALUE!>val d = foo(<!VALUE_CANNOT_BE_PROMOTED!>this<!>)<!>
    val c = foo(bb)
    val hello = Hello()
}

class Hello {
    fun foo(): String = name

    val nameLength = foo().length
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val name = "Alice"<!>
}

