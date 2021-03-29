// "Add 'inner' modifier" "false"
// ACTION: Create test
// ERROR: Object is not allowed here
class A() {
    inner class B() {
        object <caret>C
    }
}
