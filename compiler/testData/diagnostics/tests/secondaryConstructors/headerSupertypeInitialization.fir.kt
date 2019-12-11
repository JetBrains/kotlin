// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B(1) {
    constructor(): super(1)
}
