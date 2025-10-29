// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-39697
// WITH_STDLIB
// CHECK_TYPE

// FILE: Arguments.java
public class Arguments {
    public static Arguments of(Object... args) { return null; }
}

// FILE: main.kt
fun main() {
    val a = listOf("hello", "world").map(Arguments::of)
    val b = listOf("hello", "world").map { Arguments.of(it) }

    a.checkType { _<List<Arguments>>() }
    b.checkType { _<List<Arguments>>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, javaFunction, javaType, lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral,
typeParameter, typeWithExtension */
