// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-71579

class In<in T>(private var t: T, private val f: (T) -> Unit) {
    fun doIt() {
        f(t)
    }

    inner class Inner {
        fun takeT(t: T) {
            this@In.t = t
        }
    }

    fun accept(inner: <!TYPE_VARIANCE_CONFLICT_ERROR!>Inner<!>) {
        inner.takeT(t)
    }
}

fun box(): String {
    var inAny: In<Any?> = In<Any?>("str", f = { it })

    var inInt01: In<Int> = inAny
    var inInt02: In<Int> = In<Int>(42, f = { it + 100 })

    inInt01.accept(inInt02.Inner())

    inInt02.doIt() // ClassCastException: class java.lang.String cannot be cast to class java.lang.Integer

    return "OK"
}
