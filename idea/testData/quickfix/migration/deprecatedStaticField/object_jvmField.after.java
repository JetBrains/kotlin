// "Annotate property with @JvmField" "true"
import a.Obj;
import a.A;

class B {
    void bar() {
        A a = Obj.property;
        A a2 = a.Obj.property;
        A a3 = Obj.property;
    }
}
