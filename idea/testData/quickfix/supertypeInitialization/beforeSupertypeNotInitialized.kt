// "class org.jetbrains.kotlin.idea.quickfix.ChangeToConstructorInvocationFix" "false"
// ERROR: This type has a constructor, and thus must be initialized here
open class A(x : Int) {}
class B : A<caret> {}
