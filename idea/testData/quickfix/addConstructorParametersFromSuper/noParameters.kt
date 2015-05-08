// "Add constructor parameters and use them" "false"
// ACTION: Change to constructor invocation
// ERROR: This type has a constructor, and thus must be initialized here
open class Base

class C : Base<caret>