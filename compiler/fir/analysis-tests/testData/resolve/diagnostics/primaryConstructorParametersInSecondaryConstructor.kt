// RUN_PIPELINE_TILL: FRONTEND
class A(x: Int) {
    constructor(y: String, z: Int = <!UNRESOLVED_REFERENCE!>x<!>): this(z)
}
