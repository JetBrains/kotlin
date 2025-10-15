// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77986
// DIAGNOSTICS: -NOTHING_TO_INLINE

inline fun inlineFun() {
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> localFun() {}
    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> LocalClass {}

    run {
        <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> localFun2() {}
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
