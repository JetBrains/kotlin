// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(x: Outer) = 1
class Outer {
    inner class Inner {
        val prop = 1
    }

    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>Inner<!>().prop + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.Inner().prop) :
        this(x + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>Inner<!>().prop + <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>this<!>.Inner().prop)
}
