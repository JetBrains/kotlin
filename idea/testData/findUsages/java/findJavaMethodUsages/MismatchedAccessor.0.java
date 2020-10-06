// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIR_COMPARISON
public class Bar {
    public String getValue() {
        return "value";
    }

    public String <caret>getValue(String param) {
        return "value " + param;
    }
}