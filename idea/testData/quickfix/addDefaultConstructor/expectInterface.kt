// "Add default constructor to expect class" "false"
// ENABLE_MULTIPLATFORM
// ACTION: Create subclass
// ACTION: Remove constructor call
// ERROR: This class does not have a constructor

expect interface A

open class C : A<caret>()

actual interface A