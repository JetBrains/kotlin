// !DIAGNOSTICS: -UNUSED_VARIABLE

trait B {
    fun b_fun() {}
}

fun test(param: String) {

    val local_val = 4
    val bar = fun B.(fun_param: Int) {
        param.length()
        b_fun()
        val inner_bar = local_val + fun_param

        <!UNRESOLVED_REFERENCE!>bar<!>
    }

    <!UNRESOLVED_REFERENCE!>inner_bar<!>
    <!UNRESOLVED_REFERENCE!>fun_param<!>
}