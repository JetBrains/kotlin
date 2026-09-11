// EXPECTED: com.intellij.psi.impl.source.PsiParameterImpl(xx)
// FILE: MyClass.java
public class MyClass {
    public <TT> void foo(TT x<caret>x) {}
}
