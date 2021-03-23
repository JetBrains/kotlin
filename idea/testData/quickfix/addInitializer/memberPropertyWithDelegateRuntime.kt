// "Add initializer" "false"
// ACTION: Convert to ordinary property
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// WITH_RUNTIME
class A {
    <caret>val n: Int by lazy { 0 }
}