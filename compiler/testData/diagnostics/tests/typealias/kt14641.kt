// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

class A {
    public inner class B { }
    public typealias BAlias = B
}

fun f() {
    val a = A()
    a.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>BAlias<!>
}