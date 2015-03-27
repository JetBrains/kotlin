// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(val prop: Int)
class A : B {
    constructor(x: Int, y: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.prop) :
        super(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.prop)
}
