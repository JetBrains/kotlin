fun foo(arg: Boolean) {
    val x : Int
    if (arg) {
        x = 4
    } else {
        x = 2
    }

    x.hashCode()

    class Local {
        fun bar() {
            <!UNINITIALIZED_VARIABLE!>x<!>.hashCode()
        }
    }
}
