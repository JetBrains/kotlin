// "Replace with reference to 'INSTANCE' field" "true"
import a.A;

class B {
    void bar() {
        A.INSTANCE.getName();
    }
}
