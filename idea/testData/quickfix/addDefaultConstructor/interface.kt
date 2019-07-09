// "Add default constructor to expect class" "false"
// ACTION: Create subclass
// ACTION: Introduce import alias
// ACTION: Remove constructor call
// ERROR: This class does not have a constructor

interface A

open class C : A<caret>()