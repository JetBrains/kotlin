// "Add constructor parameter 'z'" "true"
abstract class A(val x: Int, val y: String, val z: Long)
class B(x: Int, y: String) : A(x, y<caret>)