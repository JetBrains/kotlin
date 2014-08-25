// FILE: p/PsiElement.java

package p;

public interface PsiElement {}

// FILE: p/JetExpression.java

package p;

public interface JetExpression extends PsiElement {}

// FILE: p/Util.java

package p;

public class Util {

    public static <T extends PsiElement> T getParentOfType(@Nullable PsiElement element, @NotNull Class<T> aClass) {
        return null;
    }

    public static void on(JetExpression e) {}
}

// FILE: k.kt

import p.*

fun test(e: JetExpression) {
    Util.on(
        Util.getParentOfType(e, javaClass<JetExpression>()) ?: e
    )
}

fun <T> javaClass(): Class<T> = null!!