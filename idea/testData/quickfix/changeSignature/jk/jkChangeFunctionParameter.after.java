// "Change 2nd parameter of method 'foo' from 'boolean' to 'String'" "true"

public class J {
    void foo() {
        new K().foo(1, <caret>"2");
    }
}