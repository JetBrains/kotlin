// "Add 'inner' modifier" "true"
class A() {
    inner class B() {
        class <caret>C() {
        }
    }
}
/* FIR_COMPARISON */
