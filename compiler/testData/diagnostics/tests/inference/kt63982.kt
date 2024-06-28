// ISSUE: KT-63982
// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST

// FILE: StubBasedPsiElement.java
public interface StubBasedPsiElement<Stub extends StubElement> extends PsiElement {
    Stub getStub();
}

// FILE: test.kt
interface PsiElement
interface JSCallExpression: PsiElement
interface StubElement<T : PsiElement>

fun test(child: PsiElement) {
    if (child is JSCallExpression) {
        val callStub = if (child is StubBasedPsiElement<*>) child.stub else null
    }
}
