// "Add 'inner' modifier" "false"
// ERROR: Annotation class is not allowed here
class A() {
    inner class B() {
        annotation class <caret>C
    }
}
