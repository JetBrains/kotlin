// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIND_BY_REF
// WITH_FILE_NAME
class J extends A {
    constructor(n: Int) {
        super(n);
    }

    static void test() {
        new A();
        new <caret>A(1);
    }
}