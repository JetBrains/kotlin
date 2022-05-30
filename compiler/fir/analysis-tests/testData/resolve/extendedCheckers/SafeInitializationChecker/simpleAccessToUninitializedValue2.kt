class A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>var a = foo()<!>

    fun foo(): String {
        a = "Hello"
        return a.substring(1)
    }
}