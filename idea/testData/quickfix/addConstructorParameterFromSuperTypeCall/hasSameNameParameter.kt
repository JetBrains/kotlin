// "Add constructor parameter 'y'" "false"
// DISABLE-ERRORS
// ACTION: Add 'x =' to argument
// ACTION: Create secondary constructor
// ACTION: Remove parameter 'y'
// ACTION: Remove parameter 'z'
abstract class A(val x: Int, val y: String, val z: Long)
class B(x: Int, y: String, z: Long) : A(x<caret>)