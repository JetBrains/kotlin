// "Add 'inner' modifier" "false"
// ACTION: Implement interface
// ERROR: Interface is not allowed here
class A() {
    inner class B() {
        interface <caret>C
    }
}
