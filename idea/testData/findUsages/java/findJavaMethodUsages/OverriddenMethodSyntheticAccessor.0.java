// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
public interface AI {
    String <caret>getFoo();

    public class A implements AI {
        @Override
        public String getFoo() {return "";}
    }
}
