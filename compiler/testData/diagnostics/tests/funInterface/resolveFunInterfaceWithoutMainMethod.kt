<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface IsolatedFunFace {
}

typealias FunAlias = IsolatedFunFace

fun referIsolatedFunFace(<!UNUSED_PARAMETER!>iff<!>: IsolatedFunFace) {}

fun callIsolatedFunFace() {
    referIsolatedFunFace(<!RESOLUTION_TO_CLASSIFIER!>IsolatedFunFace<!> {})
    referIsolatedFunFace(<!RESOLUTION_TO_CLASSIFIER!>FunAlias<!> {})
    referIsolatedFunFace(<!TYPE_MISMATCH!>{}<!>)
}