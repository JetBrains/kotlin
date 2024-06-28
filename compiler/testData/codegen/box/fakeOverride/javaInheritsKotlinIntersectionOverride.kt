// TARGET_BACKEND: JVM

// MODULE: lib
// FILE: KtWhenExpression.java
public class KtWhenExpression extends KtExpressionImpl {}

// FILE: KtExpressionImpl.kt
abstract class KtExpressionImpl : PsiElementBase(), PsiElement {}

// FILE: PsiElementBase.java
public abstract class PsiElementBase implements PsiElement {
    @Override
    public String getProject() { return ""; }
}

// FILE: PsiElement.java
public interface PsiElement {
    String getProject();
}

// MODULE: box(lib)
// FILE: box.kt
private fun test(expression: KtWhenExpression) {
    expression.project
}

fun box(): String {
    test(KtWhenExpression())
    return "OK"
}