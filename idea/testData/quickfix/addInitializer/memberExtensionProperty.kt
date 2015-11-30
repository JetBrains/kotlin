// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$AddInitializerFix" "false"
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ACTION: Make 'n' abstract
// ERROR: Property must be initialized or be abstract
class A {
    <caret>val Int.n: Int
}