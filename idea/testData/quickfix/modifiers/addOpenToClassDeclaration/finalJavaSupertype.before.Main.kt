// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ERROR: This type is final, so it cannot be inherited from
// ACTION: Create test
class foo : <caret>JavaClass() {}
