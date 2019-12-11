// See KT-13997

class Foo {
    var bar: Int // Ok
        external get
        external set
}

class Bar {
    val foo: Int // Ok
        external get

    var baz: Int
        external get

    var gav: Int
        external set
}

