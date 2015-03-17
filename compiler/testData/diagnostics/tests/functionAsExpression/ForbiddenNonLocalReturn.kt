// !DIAGNOSTICS: -UNUSED_VARIABLE

fun test() {
    fun bar() {
        val bas = fun() {
            <!RETURN_NOT_ALLOWED!>return@bar<!>
        }
    }

    val bar = fun() {
        <!RETURN_NOT_ALLOWED!>return@test<!>
    }
}

fun foo() {
    val bal = @bag fun () {
        val bar = fun() {
            <!RETURN_NOT_ALLOWED!>return@bag<!>
        }
        return@bag
    }
    val bag = fun name() {
        val bar = fun () {
            <!RETURN_NOT_ALLOWED!>return@name<!>
        }
    }
}