// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// FIR_DUMP

// FILE: J.java
public class J {
    public static <T> T id(T x) {
        return x;
    }
}

// FILE: main.kt
val String?.nullableLengthExt: Int get() = this?.length ?: 0

fun nullableExtensionChosen() {
    J.id<String?>(null).nullableLengthExt
}

fun nullableToStringStaysSafe() {
    J.id<String?>(null).toString()
}

fun nullableDirectMemberStillUnsafe() {
    J.id<String?>(null).length
}

fun nonNullBaseline() {
    J.id<String>("abc").length
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, nullableType, propertyDeclaration, stringLiteral */
