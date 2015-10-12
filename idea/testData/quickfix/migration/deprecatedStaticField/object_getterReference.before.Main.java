// "Replace with getter invocation" "true"
import a.Obj;
import a.A;

class B {
    void bar() {
        A a = Obj.pro<caret>perty;
        A a2 = Obj.getProperty();
        A a3 = Obj.property;
    }
}
