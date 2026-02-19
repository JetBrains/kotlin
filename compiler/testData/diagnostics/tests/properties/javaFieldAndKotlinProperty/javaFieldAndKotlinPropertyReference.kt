// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty
// ISSUE: KT-56386

// FILE: BaseJava.java
public class BaseJava {
    public String a = "OK";
}

// FILE: Derived.kt
class Derived : BaseJava() {
    private val a = "FAIL"
}

fun box(): String {
    val d = Derived()
    return d::a.get()
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, functionDeclaration, javaCallableReference, javaType,
localProperty, propertyDeclaration, stringLiteral */
