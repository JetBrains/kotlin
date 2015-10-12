// "Add 'const' modifier to a property" "true"
import a.A;

class B {
    void bar() {
        A a = A.pro<caret>perty;
        A a2 = A.Named.getProperty();
        A a3 = A.property;
    }
}