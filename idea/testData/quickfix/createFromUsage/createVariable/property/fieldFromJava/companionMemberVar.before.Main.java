// "Add 'var' property 'foo' to 'K'" "true"
// WITH_RUNTIME
class J {
    void test() {
        String s = K.<caret>foo;
    }
}