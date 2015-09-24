// "Replace with new-style facade class" "true"
import facade.FacadePackage;

class A {
    void bar() {
        FacadePackage.<caret>foo();
    }
}
