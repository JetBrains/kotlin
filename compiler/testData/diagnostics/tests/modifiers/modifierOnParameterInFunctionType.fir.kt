// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun f(vararg x: Int) {}

val inVal: (vararg x: Int)->Unit = {}

fun inParam(fn: (vararg x: Int)->Unit) {}

fun inParamNested(fn1: (fn2: (vararg n: Int)->Unit)->Unit) {}

fun inReturn(): (vararg x: Int)->Unit = {}

class A : (vararg Int)->Unit {
    override fun invoke(p1: Int) {
        var lambda: (vararg x: Int)->Unit = {}
    }

    val prop: (vararg x: Int)->Unit
        get(): (vararg x: Int)->Unit = {}
}

val allProhibited: (abstract
                    annotation
                    companion
                    const
                    crossinline
                    data
                    enum
                    external
                    final
                    in
                    inline
                    inner
                    internal
                    lateinit
                    noinline
                    open
                    operator
                    out
                    override
                    private
                    protected
                    public
                    reified
                    sealed
                    tailrec
                    vararg

                    x: Int)->Unit = {}

val valProhibited: (val x: Int)->Unit = {}
val varProhibited: (var x: Int)->Unit = {}