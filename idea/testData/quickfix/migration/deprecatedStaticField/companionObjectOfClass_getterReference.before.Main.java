// "Replace with getter invocation" "true"
import a.A;

class B {
    void bar() {
        A a = A.pro<caret>perty;
        A a2 = A.Named.getProperty();
        A a3 = A.property;
    }
}