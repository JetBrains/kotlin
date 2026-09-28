// EXPECTED: com.intellij.psi.impl.source.tree.java.PsiTypeParameterImpl(TT)
// FILE: MyClass.java
public class MyClass {
    public <T<caret>T> void foo(TT xx) {}
}
