// "Add method 'foo' to 'TestKt'" "true"
class J {
    void test() {
        boolean b = TestKt.<caret>foo(1, "2");
    }
}