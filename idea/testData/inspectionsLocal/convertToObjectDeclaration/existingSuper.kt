// PROBLEM: none

open class A {}
open class B {}

class <caret>Test : A() {
    companion object : B() {

    }
}
