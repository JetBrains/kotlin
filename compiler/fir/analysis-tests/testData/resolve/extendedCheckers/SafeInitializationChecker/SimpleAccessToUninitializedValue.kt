// WITH_STDLIB

class Hello {
    fun foo(): String = name

    val nameLength = foo().length
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val name = "Alice"<!>
}

class B(bb: B) {
    fun foo(b: B): String =
        if (d.isEmpty()) b.hello.name else ""

    <!ACCESS_TO_UNINITIALIZED_VALUE!>val d = foo(<!VALUE_CANNOT_BE_PROMOTED!>this<!>)<!>
    val c = foo(bb)
    val hello = Hello()
}

