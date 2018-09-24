// "Change 1st parameter of method 'K' from 'String' to 'int'" "true"

public class J {
    void foo() {
        new K<caret>(1, 2);
    }
}