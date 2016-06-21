// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
public @interface JAnn {
    String <caret>value();
}

class Test {
    @JAnn("abc")
    void test1() { }

    @JAnn(value = "abc")
    void test2() { }
}