// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77986
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun inlineFun() {
    fun localFun() {}
    class LocalClass {}

    run {
        fun localFun2() {}
    }
}

fun outerFun() {
    inline fun localInlineFun() {}
}

abstract class Base {
    abstract fun withDefault(f: () -> Unit = { -> })
}

class Derived : Base() {
    override final inline <!OVERRIDE_BY_INLINE!>fun withDefault(
            f: () -> Unit
    )<!> {}
}
