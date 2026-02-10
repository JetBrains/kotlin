// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND

class Function1Impl : (String) -> Unit {
    override fun invoke(myParamName: String) {}
}

fun test1(f: Function1Impl) {
    f("")
    f<!NO_VALUE_FOR_PARAMETER!>(<!NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    f(myParamName = "")
    f.invoke("")
    <!NO_VALUE_FOR_PARAMETER!>f.invoke(<!NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    f.invoke(myParamName = "")
}

fun test2(f: (String) -> Unit) {
    f("")
    f<!NO_VALUE_FOR_PARAMETER!>(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    f<!NO_VALUE_FOR_PARAMETER!>(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>myParamName<!> = "")<!>
    f.invoke("")
    <!NO_VALUE_FOR_PARAMETER!>f.invoke(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    <!NO_VALUE_FOR_PARAMETER!>f.invoke(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>myParamName<!> = "")<!>
}

fun test3(f: String.(String) -> Unit) {
    "".f("")
    <!NO_VALUE_FOR_PARAMETER!>"".f(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    <!NO_VALUE_FOR_PARAMETER!>"".f(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>zzz<!> = "")<!>
}

fun test4(f: (myParamName: String) -> Unit) {
    f("")
    f<!NO_VALUE_FOR_PARAMETER!>(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    f(<!NAMED_ARGUMENTS_NOT_ALLOWED!>myParamName<!> = "")
    f.invoke("")
    <!NO_VALUE_FOR_PARAMETER!>f.invoke(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>p0<!> = "")<!>
    f.invoke(<!NAMED_ARGUMENTS_NOT_ALLOWED!>myParamName<!> = "")
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, operator, override, stringLiteral,
typeWithExtension */
