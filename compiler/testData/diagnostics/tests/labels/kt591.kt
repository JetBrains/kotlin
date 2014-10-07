//KT-591 Unresolved label in valid code

fun test() {
    val <!UNUSED_VARIABLE!>a<!> = @a{(Int?).() ->
        if (this != null) {
            val <!UNUSED_VARIABLE!>b<!> = {String.() ->
                <!DEBUG_INFO_SMARTCAST!>this@a<!>.times(5) // @a Unresolved
            }
        }
    }
}