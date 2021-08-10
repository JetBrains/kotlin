// FILE: ObjectPattern.java

import org.jetbrains.annotations.NotNull;

public abstract class ObjectPattern<T, Self extends ObjectPattern<T, Self>> {
    protected ObjectPattern(@NotNull Class<T> aClass) {

    }

    public static class Capture<T> extends ObjectPattern<T,Capture<T>> {
        public Capture(@NotNull Class<T> aClass) {
            super(aClass);
        }
    }
}

// FILE: UastPatterns.kt

interface UElement

interface UExpression : UElement

interface UReferenceExpression : UExpression

fun injectionHostOrReferenceExpression(): UExpressionPattern.Capture<UExpression> =
    uExpression().filter { it is UReferenceExpression }

fun uExpression(): UExpressionPattern.Capture<UExpression> = expressionCapture(UExpression::class.java)

fun <T : UExpression> expressionCapture(clazz: Class<T>): UExpressionPattern.Capture<T> = UExpressionPattern.Capture(clazz)

open class UElementPattern<T : UElement, Self : UElementPattern<T, Self>>(clazz: Class<T>) : ObjectPattern<T, Self>(clazz) {
    fun filter(filter: (T) -> Boolean): Self = this as Self
}

open class UExpressionPattern<T : UExpression, Self : UExpressionPattern<T, Self>>(clazz: Class<T>) : UElementPattern<T, Self>(clazz) {
    open class Capture<T : UExpression>(clazz: Class<T>) : UExpressionPattern<T, Capture<T>>(clazz)
}