// "Cleanup code" "true"
import a.*;

class B {
    void bar() {
        Cl.prope<caret>rty1;
        Cl.property2;
        Cl.Companion.getProperty1();
        Cl.Companion.getProperty2();
        Int.property1;
        Int.property2;
        Int.Companion.getProperty1();
        Int.Companion.getProperty2();
        Obj.property1;
        Obj.property2;
        Obj.INSTANCE.getProperty1();
        Obj.INSTANCE.getProperty2();
    }
}