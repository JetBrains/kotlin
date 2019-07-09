<!NOTHING_TO_INLINE!>inline<!> fun inlineFun() {
    <!NOT_YET_SUPPORTED_IN_INLINE("Local functions")!>fun<!> localFun() {}
    <!NOT_YET_SUPPORTED_IN_INLINE("Local classes")!>class<!> LocalClass {}
}

fun outerFun() {
    <!NOT_YET_SUPPORTED_IN_INLINE("Local inline functions")!>inline<!> fun localInlineFun() {}
}

abstract class Base {
    abstract fun withDefault(f: () -> Unit = { -> })
}

class Derived : Base() {
    <!OVERRIDE_BY_INLINE!>override final inline fun withDefault(
            <!NOT_YET_SUPPORTED_IN_INLINE("Functional parameters with inherited default values")!>f: () -> Unit<!>
    )<!> {}
}