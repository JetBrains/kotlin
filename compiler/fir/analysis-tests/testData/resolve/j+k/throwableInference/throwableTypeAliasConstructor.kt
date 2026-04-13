// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +InferThrowableTypeParameterToUpperBound
// FIR_DUMP
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
//  ^ difference in DEBUG_INFO_EXPRESSION_TYPE
// ISSUE: KT-82961

// FILE: ThrowableComputable.java
public interface ThrowableComputable<R, X extends Throwable> {
    R compute() throws X;
}

// FILE: JavaRunner.java
public class JavaRunner<V, E extends Throwable> {
    public JavaRunner(ThrowableComputable<V, E> action) throws E {
    }
}

// FILE: test.kt
typealias AliasedRunner<W, F> = JavaRunner<W, F>
typealias SimpleRunner<U> = JavaRunner<U, RuntimeException>

fun test() {
    // We don't allow inferring exception-related type variables there, because they're mentioned in the return types of the constructors
    val r1 = <!CANNOT_INFER_PARAMETER_TYPE!>JavaRunner<!> { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<(kotlin.String..kotlin.String?), ERROR CLASS: Cannot infer argument for type parameter E>")!>r1<!>
    val r2 = <!CANNOT_INFER_PARAMETER_TYPE!>AliasedRunner<!> { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<(kotlin.String..kotlin.String?), ERROR CLASS: Cannot infer argument for type parameter F>")!>r2<!>
    val r3 = SimpleRunner { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<(kotlin.String..kotlin.String?), java.lang.RuntimeException>")!>r3<!>

    val r4 = JavaRunner<String, RuntimeException> { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<kotlin.String, java.lang.RuntimeException>")!>r4<!>
    val r5 = AliasedRunner<String, RuntimeException> { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<kotlin.String, java.lang.RuntimeException>")!>r5<!>
    val r6 = SimpleRunner<String>  { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<kotlin.String, java.lang.RuntimeException>")!>r6<!>

    val r7 = JavaRunner<String, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>> { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<kotlin.String, ERROR CLASS: Cannot infer argument for type parameter E>")!>r7<!>
    val r8 = AliasedRunner<String, <!CANNOT_INFER_PARAMETER_TYPE!>_<!>> { "hello" }
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaRunner<kotlin.String, ERROR CLASS: Cannot infer argument for type parameter F>")!>r8<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, samConversion, stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
