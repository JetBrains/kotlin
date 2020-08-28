interface Foo {
    val foo: suspend () -> Unit
}

interface Bar<T> {
    val bar: T
}

class Test1 : Foo {
    override val foo = <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>{}<!>
}

class Test2 : Foo {
    override val foo: suspend () -> Unit = {}
}

class Test3 : Bar<suspend () -> Unit> {
    override val bar = <!PROPERTY_TYPE_MISMATCH_ON_OVERRIDE!>{}<!>
}

class Test4 : Bar<suspend () -> Unit> {
    override val bar: suspend () -> Unit = {}
}