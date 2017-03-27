// "class org.jetbrains.kotlin.idea.quickfix.AddModifierFix" "false"
// ACTION: Create test
// ACTION: Inline type parameter
// ACTION: Remove final upper bound
class foo<T : <caret>JavaClass>() {}
