// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int)
class A : B {
    val prop = 1
    constructor(x: Int, y: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop) :
        super(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop)
}
