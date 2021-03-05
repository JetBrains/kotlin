<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface IsolatedFunFace {
}

typealias FunAlias = IsolatedFunFace

fun referIsolatedFunFace(iff: IsolatedFunFace) {}

fun callIsolatedFunFace() {
    referIsolatedFunFace(<!UNRESOLVED_REFERENCE!>IsolatedFunFace<!> {})
    referIsolatedFunFace(<!UNRESOLVED_REFERENCE!>FunAlias<!> {})
    <!INAPPLICABLE_CANDIDATE!>referIsolatedFunFace<!>({})
}
