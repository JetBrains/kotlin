// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>B(1)<!> {
    constructor(): super(1) {}
}
