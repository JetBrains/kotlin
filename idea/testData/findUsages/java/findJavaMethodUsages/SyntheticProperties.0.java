// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
class JavaClass {
    public int <caret>getSomething() { return 1; }
    public void setSomething(int value) {}
}