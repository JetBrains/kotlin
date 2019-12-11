// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Double) {
    constructor(x: Int): this(1.0)
    constructor(x: String): this(1.0)
}
interface C
class A : B, C {
    constructor(): <!INAPPLICABLE_CANDIDATE!>super<!>(' ')
    constructor(x: Int)
}
