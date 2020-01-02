// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun f(x: Int = 0) {}

val inVal: (x: Int = 0)->Unit = {}

fun inParam(fn: (x: Int = 0)->Unit) {}

fun inParamNested(fn1: (fn2: (n: Int = 0)->Unit)->Unit) {}

fun inReturn(): (x: Int = 0)->Unit = {}

class A : (Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (x: Int = 0)->Unit = {}
    }

    val prop: (x: Int = 0)->Unit
        get(): (x: Int = 0)->Unit = {}
}
