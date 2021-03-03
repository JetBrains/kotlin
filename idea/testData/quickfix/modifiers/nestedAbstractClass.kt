// "Add 'inner' modifier" "true"
class A() {
    inner class B() {
        abstract class <caret>C
    }
}
/* FIR_COMPARISON */
