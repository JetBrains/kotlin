// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$MoveToConstructorParameters" "false"
// ACTION: Make 'n' abstract
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ERROR: Extension property must have accessors or be abstract
class A {
    <caret>val Int.n: Int
}