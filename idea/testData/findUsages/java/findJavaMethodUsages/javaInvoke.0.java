// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages

public class JavaClassInvoke {
    public void <caret>invoke() {
    }

    public static JavaClassInvoke INSTANCE = new JavaClassInvoke();

    public static class Other extends JavaClassInvoke {}
    public static class AnotherOther extends Other {}

    public static class JavaOther {
        public void invoke() {
        }

        public static JavaOther INSTANCE = new JavaOther();
    }

    public static class OtherJavaClass extends JavaClassInvoke {
        public static OtherJavaClass OJC = new OtherJavaClass();
    }
}