// "Add method 'foo' to 'K'" "true"
// WITH_RUNTIME
class J {
    void test() {
        boolean b = K.<caret>foo(1, "2");
    }
}