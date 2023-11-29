// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

annotation class Ann
annotation class Ann2

fun f(@Ann x: Int) {}

val inVal: (<!UNSUPPORTED!>@Ann<!> <!UNSUPPORTED!>@Ann2<!> x: Int)->Unit = {}

fun inParam(fn: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (<!UNSUPPORTED!>@Ann<!> n: Int)->Unit)->Unit) {}

fun inReturn(): (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}

class A : (<!WRONG_ANNOTATION_TARGET!>@Ann<!> Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}
    }

    val prop: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit
        get(): (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}
}

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

val onType: (@TypeAnn A).(<!UNSUPPORTED!>@Ann<!> a: @TypeAnn A, @TypeAnn A)->@TypeAnn A? = <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>{<!> null }

fun (@TypeAnn A).extFun(@Ann a: @TypeAnn A): @TypeAnn A? = null

@Target(AnnotationTarget.TYPE)
annotation class TypeAnnWithArg(val arg: String)

fun badArgs(a: (@TypeAnnWithArg(<!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>unresolved<!> = "")<!> Int) -> Unit) {}

typealias BadArgsInTypeAlias = (@<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> Int) -> Unit
fun badArgsInTypeAlias(a: BadArgsInTypeAlias) {}

typealias T<X> = (X) -> Unit
fun badArgsInTypeAliasInstance(a: T<@TypeAnnWithArg(arg = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>123<!>) Int>) {}

typealias BadArgsInRecursive = (((@<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> Int) -> Unit) -> @<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> String) -> Unit

typealias BadArgsMultiple = (@<!NO_VALUE_FOR_PARAMETER!>TypeAnnWithArg<!> Int, @TypeAnnWithArg(arg = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>123<!>) Int) -> Unit
