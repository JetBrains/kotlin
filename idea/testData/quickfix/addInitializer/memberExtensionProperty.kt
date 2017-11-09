// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$AddInitializerFix" "false"
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ACTION: Make 'n' abstract
// ERROR: Extension property must have accessors or be abstract
class A {
    <caret>val Int.n: Int
}