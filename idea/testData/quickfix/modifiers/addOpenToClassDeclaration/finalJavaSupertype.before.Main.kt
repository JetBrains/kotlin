// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ERROR: This type is final, so it cannot be inherited from
// ACTION: Create test
// ACTION: Introduce import alias
class foo : <caret>JavaClass() {}
