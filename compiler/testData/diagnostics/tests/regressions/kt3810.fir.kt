interface A {
    var foo: String
}

class B(override val foo: String) : A
