// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: 1.kt
fun <S> Array<S>.plus(): Array<S> {
    val result = Arrays.copyOf(this, 3)
    // result type is Array<(out) (S&Any)!>!
    return result
}

// FILE: Arrays.java
public class Arrays {
    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) null;
    }
}

/* GENERATED_FIR_TAGS: flexibleType, funWithExtensionReceiver, functionDeclaration, integerLiteral, javaFunction,
localProperty, nullableType, propertyDeclaration, thisExpression, typeParameter */
