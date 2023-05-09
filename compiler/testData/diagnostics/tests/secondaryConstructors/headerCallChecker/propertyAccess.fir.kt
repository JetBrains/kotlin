// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    val prop = 1
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop) :
        this(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop<!> <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.prop)
}
