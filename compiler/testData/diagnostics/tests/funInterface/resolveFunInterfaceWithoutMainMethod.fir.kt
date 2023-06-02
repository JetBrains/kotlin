<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface IsolatedFunFace {
}

typealias FunAlias = IsolatedFunFace

fun referIsolatedFunFace(iff: IsolatedFunFace) {}

fun callIsolatedFunFace() {
    referIsolatedFunFace(<!INTERFACE_AS_FUNCTION!>IsolatedFunFace<!> {})
    referIsolatedFunFace(<!INTERFACE_AS_FUNCTION!>FunAlias<!> {})
    referIsolatedFunFace(<!ARGUMENT_TYPE_MISMATCH!>{}<!>)
}
