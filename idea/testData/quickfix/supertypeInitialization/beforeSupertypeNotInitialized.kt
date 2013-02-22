// "Change to constructor invocation" "false"
// ERROR: This type has a constructor, and thus must be initialized here
open class A(x : Int) {}
class B : A<caret> {}
