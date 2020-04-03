class Function1Impl : (String) -> Unit {
    override fun invoke(myParamName: String) {}
}

fun test1(f: Function1Impl) {
    f("")
    <!INAPPLICABLE_CANDIDATE!>f<!>(p0 = "")
    f(myParamName = "")
    f.invoke("")
    f.<!INAPPLICABLE_CANDIDATE!>invoke<!>(p0 = "")
    f.invoke(myParamName = "")
}

fun test2(f: (String) -> Unit) {
    f("")
    <!INAPPLICABLE_CANDIDATE!>f<!>(p0 = "")
    <!INAPPLICABLE_CANDIDATE!>f<!>(myParamName = "")
    f.invoke("")
    f.<!INAPPLICABLE_CANDIDATE!>invoke<!>(p0 = "")
    f.<!INAPPLICABLE_CANDIDATE!>invoke<!>(myParamName = "")
}

fun test3(f: String.(String) -> Unit) {
    "".f("")
    "".<!INAPPLICABLE_CANDIDATE!>f<!>(p0 = "")
    "".<!INAPPLICABLE_CANDIDATE!>f<!>(zzz = "")
}
