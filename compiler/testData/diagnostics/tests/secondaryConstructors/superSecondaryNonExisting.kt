// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Double) {
    constructor(x: Int): this(1.0)
    constructor(x: String): this(1.0)
}
interface C
class A : B, C {
    constructor(): <!NONE_APPLICABLE!>super<!>(' ')
    <!EXPLICIT_DELEGATION_CALL_REQUIRED!>constructor(x: Int)<!>
}
