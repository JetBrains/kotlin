// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    fun foo() = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo()) :
        this(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>foo<!>() <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.foo())
}
