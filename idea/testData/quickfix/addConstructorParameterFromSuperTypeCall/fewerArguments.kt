// "Add constructor parameter 'z'" "false"
// DISABLE-ERRORS
// ACTION: Add 'x =' to argument
// ACTION: Add constructor parameter 'y'
// ACTION: Create secondary constructor
// ACTION: Remove parameter 'y'
// ACTION: Remove parameter 'z'
abstract class A(val x: Int, val y: String, val z: Long)
class B(x: Int) : A(x<caret>)