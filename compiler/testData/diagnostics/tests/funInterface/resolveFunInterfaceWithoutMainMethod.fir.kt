fun interface IsolatedFunFace {
}

typealias FunAlias = IsolatedFunFace

fun referIsolatedFunFace(iff: IsolatedFunFace) {}

fun callIsolatedFunFace() {
    referIsolatedFunFace(<!UNRESOLVED_REFERENCE!>IsolatedFunFace<!> {})
    referIsolatedFunFace(<!UNRESOLVED_REFERENCE!>FunAlias<!> {})
    referIsolatedFunFace(<!ARGUMENT_TYPE_MISMATCH!>{}<!>)
}