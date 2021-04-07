// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

annotation class Ann

fun f(@Ann x: Int) {}

val inVal: (@Ann x: Int)->Unit = {}

fun inParam(fn: (@Ann x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (@Ann n: Int)->Unit)->Unit) {}

fun inReturn(): (@Ann x: Int)->Unit = {}

class A : (@Ann Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (@Ann x: Int)->Unit = {}
    }

    val prop: (@Ann x: Int)->Unit
        get(): (@Ann x: Int)->Unit = {}
}

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

// next code is invalid on modern compiller's version;
// due it InitializerTypeMismatch checker reports here
val onType: (@TypeAnn A).(@Ann a: @TypeAnn A, @TypeAnn A)->@TypeAnn A? = <!INITIALIZER_TYPE_MISMATCH!>{ null }<!>

fun (@TypeAnn A).extFun(@Ann a: @TypeAnn A): @TypeAnn A? = null