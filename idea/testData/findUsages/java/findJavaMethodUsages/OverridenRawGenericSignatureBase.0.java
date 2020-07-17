// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: overrides
public interface Foo {
    <T> void <caret>baz(T foo);
}