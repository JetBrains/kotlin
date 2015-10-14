// "Add 'const' modifier to a property" "true"
import a.A;

class B {
    void bar() {
        A a = A.property;
        A a2 = a.A.property;
        A a3 = A.property;
    }
}