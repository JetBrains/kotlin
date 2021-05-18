// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

annotation class Ann

fun f(@Ann x: Int) {}

val inVal: (@Ann x: Int)->Unit = {}

fun inParam(fn: (@Ann x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (@Ann n: Int)->Unit)->Unit) {}

fun inReturn(): (@Ann x: Int)->Unit = {}

class A : (<!WRONG_ANNOTATION_TARGET!>@Ann<!> Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (@Ann x: Int)->Unit = {}
    }

    val prop: (@Ann x: Int)->Unit
        get(): (@Ann x: Int)->Unit = {}
}

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

val onType: (@TypeAnn A).(@Ann a: @TypeAnn A, @TypeAnn A)->@TypeAnn A? = { null }

fun (@TypeAnn A).extFun(@Ann a: @TypeAnn A): @TypeAnn A? = null