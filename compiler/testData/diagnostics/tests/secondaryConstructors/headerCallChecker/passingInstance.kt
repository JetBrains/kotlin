// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(x: A) = 1

class A {
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + foo(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>) + foo(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>)) :
        this(x + foo(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>) + foo(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this@A<!>))
}
