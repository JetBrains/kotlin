class Function1Impl : (String) -> Unit {
    override fun invoke(myParamName: String) {}
}

fun test1(f: Function1Impl) {
    f("")
    f(<!NAMED_PARAMETER_NOT_FOUND!>p0<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f(myParamName = "")
    f.invoke("")
    f.invoke(<!NAMED_PARAMETER_NOT_FOUND!>p0<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.invoke(myParamName = "")
}

fun test2(f: (String) -> Unit) {
    f("")
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>myParamName<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.invoke("")
    f.invoke(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    f.invoke(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>myParamName<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
}

fun test3(f: String.(String) -> Unit) {
    "".f("")
    "".f(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
    "".f(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>zzz<!> = ""<!NO_VALUE_FOR_PARAMETER!>)<!>
}
