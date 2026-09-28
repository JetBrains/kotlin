// EXPECTED: com.intellij.psi.impl.source.PsiEnumConstantImpl(AA)
// FILE: MyClass.java
enum MyClass {
    A<caret>A {
        public void bar() {}
    },
    BB,
    CC
}
