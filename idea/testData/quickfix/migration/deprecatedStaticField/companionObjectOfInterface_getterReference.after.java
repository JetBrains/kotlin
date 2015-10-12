// "Replace with getter invocation" "true"
import a.A;

class B {
    void bar() {
        A a = a.A.Companion.getProperty();
        A a2 = A.Companion.getProperty();
        A a3 = A.property;
    }
}