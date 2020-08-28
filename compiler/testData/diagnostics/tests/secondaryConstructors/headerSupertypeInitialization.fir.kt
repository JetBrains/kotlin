// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
<!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR!>class A : B(1) {
    constructor(): super(1)
}<!>
