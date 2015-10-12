// "Annotate property with @JvmField" "true"
import a.A;

class B {
    void bar() {
        A a = A.pro<caret>perty;
        A a2 = A.Companion.getProperty();
        A a3 = A.property;
    }
}
