// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
class A {
    public void <caret>foo() {

    }
}

class B extends A {
    @Override
    public void foo() {

    }
}

// FIR_COMPARISON