// "class org.jetbrains.kotlin.idea.quickfix.InitializePropertyQuickFixFactory$AddInitializerFix" "false"
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
class A {
    <caret>val n: Int by lazy { 0 }
}