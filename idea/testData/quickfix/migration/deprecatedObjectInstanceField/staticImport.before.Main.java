// "Replace with reference to 'INSTANCE' field" "true"
import static a.A.INSTANCE$;

class B {
    void bar() {
        IN<caret>STANCE$.getName();
    }
}
