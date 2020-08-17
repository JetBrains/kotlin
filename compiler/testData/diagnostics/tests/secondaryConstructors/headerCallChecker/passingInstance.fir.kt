// !DIAGNOSTICS: -UNUSED_PARAMETER
fun foo(x: A) = 1

class A {
    constructor(x: Int)
    constructor(x: Int, y: Int, z: Int = x + foo(<!NO_THIS!>this<!>) + foo(<!UNRESOLVED_LABEL!>this@A<!>)) :
        this(x + foo(this) + foo(this@A))
}
