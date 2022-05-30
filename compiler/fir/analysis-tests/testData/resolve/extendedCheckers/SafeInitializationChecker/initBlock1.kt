// WITH_STDLIB

class B {
    var a: String

    init {
        a = "Hello"
        a = a.substring(1)
    }
}

class C {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val x: Any<!>
    init {
        x = foo()
    }
    private fun foo() = x.hashCode()
}
