class O : Function2<Int, String, Unit> {
    override fun invoke(p1: Int, p2: String) {
    }
}

fun test() {
    val a = fun(<!UNUSED_ANONYMOUS_PARAMETER!>o<!>: O) {
    }
    a <!TYPE_MISMATCH!>{}<!>
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Ext<!> : <!SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE!>String.() -> Unit<!> {
}

fun test2() {
    val <!UNUSED_VARIABLE!>f<!>: Ext = <!TYPE_MISMATCH!>{}<!>
}
