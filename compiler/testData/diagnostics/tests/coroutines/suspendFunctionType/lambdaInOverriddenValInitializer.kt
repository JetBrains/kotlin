interface Foo {
    val foo: suspend () -> Unit
}

interface Bar<T> {
    val bar: T
}

class Test1 : Foo {
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>foo<!> = {}
}

class Test2 : Foo {
    override val foo: suspend () -> Unit = {}
}

class Test3 : Bar<suspend () -> Unit> {
    override val <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>bar<!> = {}
}

class Test4 : Bar<suspend () -> Unit> {
    override val bar: suspend () -> Unit = {}
}