// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages

public class JavaClass {
    public void <caret>invoke() {
    }

    public static class OtherJavaClass extends JavaClass {
        public static OtherJavaClass OJC = new OtherJavaClass();
    }
}