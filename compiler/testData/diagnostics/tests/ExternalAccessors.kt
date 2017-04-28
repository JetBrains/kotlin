// See KT-13997

class Foo {
    var bar: Int // Ok
        external get
        external set
}

class Bar {
    val foo: Int // Ok
        external get

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var baz: Int<!>
        external get

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>var gav: Int<!>
        external set
}

