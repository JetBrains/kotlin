// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

annotation class Ann

fun f(@Ann x: Int) {}

val inVal: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}

fun inParam(fn: (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (<!UNSUPPORTED!>@Ann<!> n: Int)->Unit)->Unit) {}

fun inReturn(): (<!UNSUPPORTED!>@Ann<!> x: Int)->Unit = {}

class A : (@Ann Int)->Unit {
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