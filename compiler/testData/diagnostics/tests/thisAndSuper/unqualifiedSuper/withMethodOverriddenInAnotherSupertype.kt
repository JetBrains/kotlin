interface A {
    fun foo() {}
}

abstract class C : A {
    override abstract fun foo()
}

interface Unrelated {
    fun foo() {}
}

class Test1 : C(), A {
    override fun foo() {
        // Abstract 'foo' defined in 'C' wins against non-abstract 'foo' defined in 'A',
        // because 'C' is a subclass of 'A' (and 'C::foo' overrides 'A::foo'),
        // even though 'A' is explicitly listed in supertypes list for 'D'.
        super.<!ABSTRACT_SUPER_CALL!>foo<!>()
    }
}

class Test2 : C(), A, Unrelated {
    override fun foo() {
        // This is ok, because there's a non-abstract 'foo' in 'Unrelated',
        // which is not overridden by abstract 'foo' in 'C'.
        super.foo()
        super<Unrelated>.foo()
    }
}