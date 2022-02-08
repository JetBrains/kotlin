abstract class A {
    abstract class Nested
}

typealias TA = A

//        constructor A()
//        │
class B : TA() {
//                    constructor A.Nested()
//                    │
    class NestedInB : Nested()
}
