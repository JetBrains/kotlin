typealias TopLevel = Any

interface A {
    typealias Nested = Any
}

class C {
    typealias Nested = Any
    class D {
        typealias Nested = Any
        fun foo() {
            typealias LocalInMember = Any
        }
    }
}

fun foo() {
    typealias Local = Any
}