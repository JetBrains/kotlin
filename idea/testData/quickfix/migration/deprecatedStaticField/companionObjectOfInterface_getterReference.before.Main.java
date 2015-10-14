// "Replace with getter invocation" "true"
import a.A;

class B {
    void bar() {
        A a = A.prop<caret>erty;
        A a2 = A.Companion.getProperty();
        A a3 = A.property;
    }
}