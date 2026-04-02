// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: J.java
public interface J<T> {
    int apply(T x);
}

// FILE: Use.java
public class Use {
    public static <T> int run(J<T> j) {
        return 0;
    }
}

// FILE: main.kt
val String?.safeLengthExt: Int get() = this?.length ?: 0

fun samConversionNullable() {
    Use.run<String?> { x ->
        x<!UNSAFE_CALL!>.<!>length
    }
}

fun samConstructorNullable() {
    val j = J<String?> { x ->
        x<!UNSAFE_CALL!>.<!>length
    }
}

fun samNullableExtensionResolution() {
    Use.run<String?> { x ->
        x.safeLengthExt
    }
}

fun samNonNullBaseline() {
    Use.run<String> { x ->
        x.length
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, samConversion */
