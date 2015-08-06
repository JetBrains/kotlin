// TARGET_CLASS: A
open class A {

}

open class B: A() {

}

class <caret>C: B() {
    // INFO: {"checked": "true"}
    val x: Int
}