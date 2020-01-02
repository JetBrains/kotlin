// !DIAGNOSTICS: -UNUSED_PARAMETER
class A(x: Int) {
    constructor()
}
open class B(x: Int)
class C(x: Int) : B(x) {
    constructor(): super(1)
}
