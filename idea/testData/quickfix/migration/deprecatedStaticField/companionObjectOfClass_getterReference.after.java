// "Replace with getter invocation" "true"
import a.A;

class B {
    void bar() {
        A a = a.A.Named.getProperty();
        A a2 = A.Named.getProperty();
        A a3 = A.property;
    }
}