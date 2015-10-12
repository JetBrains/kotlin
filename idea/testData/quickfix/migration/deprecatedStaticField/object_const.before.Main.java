// "Add 'const' modifier to a property" "true"
import a.Obj;
import a.A;

class B {
    void bar() {
        A a = Obj.pro<caret>perty;
        A a2 = Obj.INSTANCE.getProperty();
        A a3 = Obj.property;
    }
}
