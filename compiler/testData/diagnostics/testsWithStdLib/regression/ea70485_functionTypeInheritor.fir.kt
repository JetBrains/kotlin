class O : Function2<Int, String, Unit> {
    override fun invoke(p1: Int, p2: String) {
    }
}

fun test() {
    val a = fun(o: O) {
    }
    <!INAPPLICABLE_CANDIDATE!>a<!> {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Ext<!> : String.() -> Unit {
}

fun test2() {
    val f: Ext = {}
}
