// LL_FIR_DIVERGENCE
// LL FIR diagnostics tests don't run the JVM backend, so its diagnostics are not reported: `ACCIDENTAL_OVERRIDE`, `CONFLICTING_INHERITED_JVM_DECLARATIONS`.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +ContextParameters
// IGNORE_ERRORS
// FILE: JavaInterface.java
public interface JavaInterface {
    String getFoo(String a, String b);
    String bar(String a, String b, String c);
}

// FILE: KotlinContextAndExtensionInterface.kt
interface KotlinContextAndExtensionInterface {
    context(a: String)
    val String.foo: String

    context(a: String)
    fun String.bar(b: String): String
}

// FILE: test.kt
interface Intersection : KotlinContextAndExtensionInterface, JavaInterface

interface IntersectionWithOverride : KotlinContextAndExtensionInterface, JavaInterface {
    context(a: String)
    override val String.foo: String
        get() = ""

    context(a: String)
    override fun String.bar(b: String): String
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, getter,
interfaceDeclaration, javaType, override, propertyDeclaration, propertyDeclarationWithContext,
propertyWithExtensionReceiver, stringLiteral */
