// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$MoveToConstructorParameters" "false"
// ACTION: Initialize property 'n'
// ACTION: Make 'n' abstract
// ACTION: Make internal
// ACTION: Make private
// ERROR: Property must be initialized or be abstract
object A {
    <caret>val n: Int
}