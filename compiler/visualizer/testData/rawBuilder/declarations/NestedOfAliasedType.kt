abstract class A {
    abstract class Nested
}

typealias TA = A

//        fun TA.<init>(): TA /* = A */
//        │
class B : TA() {
//                    constructor A.Nested()
//                    │
    class NestedInB : Nested()
}
