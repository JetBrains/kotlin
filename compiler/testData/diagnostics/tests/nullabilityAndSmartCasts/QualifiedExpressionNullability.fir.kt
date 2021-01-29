// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
class Foo {
    fun foo(a: Foo): Foo = a
}

fun main() {
    val x: Foo? = null
    val y: Foo? = null
    
    x<!UNSAFE_CALL!>.<!>foo(y)
    x!!.<!INAPPLICABLE_CANDIDATE!>foo<!>(y)
    x.foo(y!!)
    x!!.foo(y!!)
    
    val a: Foo? = null
    val b: Foo? = null
    val c: Foo? = null
    
    a<!UNSAFE_CALL!>.<!>foo(b<!UNSAFE_CALL!>.<!>foo(c))
    a!!.foo(b<!UNSAFE_CALL!>.<!>foo(c))
    a.foo(b!!.<!INAPPLICABLE_CANDIDATE!>foo<!>(c))
    a!!.foo(b!!.<!INAPPLICABLE_CANDIDATE!>foo<!>(c))
    a.foo(b.foo(c!!))
    a!!.foo(b.foo(c!!))
    a.foo(b!!.foo(c!!))
    a!!.foo(b!!.foo(c!!))
    
    val z: Foo? = null
    z!!.foo(z!!)
}
