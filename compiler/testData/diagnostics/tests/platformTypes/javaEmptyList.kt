// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

// FILE: Collections.java
import java.util.List;

public class Collections {
    public static final <T> List<T> emptyList() {
        return null;
    }
}

// FILE: 1.kt

fun bar(): List<String> = null!!

fun test() {
    val f = if (true) {
        Collections.emptyList()
    }
    else {
        bar()
    }

    checkSubtype<List<String>>(f)
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration,
functionalType, ifExpression, infix, javaFunction, localProperty, nullableType, propertyDeclaration, typeParameter,
typeWithExtension */
