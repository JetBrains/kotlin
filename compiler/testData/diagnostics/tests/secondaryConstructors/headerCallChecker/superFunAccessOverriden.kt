// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B(x: Int) {
    open fun foo() = 1
}
class A : B {
    override fun foo() = 2
    constructor(x: Int, y: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.foo()) :
        super(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo() + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>super<!>.foo())
}
