<!NOTHING_TO_INLINE!>inline<!> fun inlineFun() {
    <!NOT_YET_SUPPORTED_IN_INLINE!>fun<!> localFun() {}
    <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> LocalClass {}
}

fun outerFun() {
    <!NOT_YET_SUPPORTED_LOCAL_INLINE_FUNCTION!>inline<!> fun localInlineFun() {}
}

abstract class Base {
    abstract fun withDefault(f: () -> Unit = { -> })
}

class Derived : Base() {
    <!OVERRIDE_BY_INLINE!>override final inline fun withDefault(
            <!NOT_YET_SUPPORTED_IN_INLINE!>f: () -> Unit<!>
    )<!> {}
}
