class A(bool: Boolean) {

    fun foo() = a

    var b: String
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val a: String<!>

    init {
        var localA: String = foo()

        if (bool) {
            b = foo()
            localA = b
            a = localA
        } else {
            a = localA
        }

        b = a
    }

}
