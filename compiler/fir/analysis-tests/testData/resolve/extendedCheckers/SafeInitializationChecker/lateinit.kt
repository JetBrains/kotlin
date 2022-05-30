class A(bool: Boolean) {
    <!ACCESS_TO_UNINITIALIZED_VALUE!><!UNNECESSARY_LATEINIT!>lateinit<!> var a: String<!>
    var b: String

    fun foo(aa: A) {
        a
    }

    init {
        if (bool) {
            a = "  "
            b = a
        } else {
            b = ""
        }
        foo(<!VALUE_CANNOT_BE_PROMOTED!>this<!>)
        a = b
        foo(this)
    }
}