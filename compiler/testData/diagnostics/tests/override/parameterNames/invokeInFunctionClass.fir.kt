class Function1Impl : (String) -> Unit {
    override fun invoke(myParamName: String) {}
}

fun test1(f: Function1Impl) {
    f("")
    f(p0 = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f(myParamName = "")
    f.invoke("")
    f.invoke(p0 = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.invoke(myParamName = "")
}

fun test2(f: (String) -> Unit) {
    f("")
    f(p0 = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f(myParamName = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.invoke("")
    f.invoke(p0 = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.invoke(myParamName = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
}

fun test3(f: String.(String) -> Unit) {
    "".f("")
    "".f(p0 = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    "".f(zzz = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
}
