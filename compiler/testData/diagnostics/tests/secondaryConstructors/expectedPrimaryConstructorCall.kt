// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(x: Int) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor<!>() {}
}
open class B(x: Int)
class C(x: Int) : B(x) {
    constructor(): <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>super<!>(1) {}
}
