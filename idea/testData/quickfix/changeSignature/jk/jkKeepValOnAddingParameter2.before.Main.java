// "Add 'int' as 2nd parameter to method 'Foo'" "true"

public class J {
    void test() {
        new Foo(<caret>1, 2);
    }
}
