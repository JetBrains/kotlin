// RUN_PIPELINE_TILL: BACKEND
abstract class A {
    abstract class Nested
}

typealias TA = A

class B : TA() {
    class NestedInB : Nested()
}
