// "Make 'foo' not abstract" "true"
class A() {
    <caret>abstract fun foo() {}
}
/* FIR_COMPARISON */