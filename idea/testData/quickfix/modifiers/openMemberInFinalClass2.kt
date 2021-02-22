// "Make 'foo' not open" "true"
class A() {
    <caret>open fun foo() {}
}
/* FIR_COMPARISON */
