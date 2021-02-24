// "Add 'int' as 1st parameter to constructor 'Foo'" "true"

public class J {
    void test() {
        new Foo(<caret>1, 2);
    }
}
