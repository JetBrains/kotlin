// "Replace with new-style facade class" "true"
import static facade.FacadePackage.foo;

class A {
    void bar() {
        <caret>foo();
    }
}
