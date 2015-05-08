// "class org.jetbrains.kotlin.idea.quickfix.SuperClassNotInitialized$AddParametersFix" "false"
// ACTION: Change to constructor invocation
// ERROR: Unresolved reference: XXX
// ERROR: This type has a constructor, and thus must be initialized here
open class Base(p1: XXX)

class C : Base<caret>