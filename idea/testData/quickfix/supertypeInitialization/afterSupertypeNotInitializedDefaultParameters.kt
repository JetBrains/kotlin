// "Change to constructor invocation" "true"
open class A(x : Int = 42, vararg y : Int) {}
class B() : A<caret>() {}
