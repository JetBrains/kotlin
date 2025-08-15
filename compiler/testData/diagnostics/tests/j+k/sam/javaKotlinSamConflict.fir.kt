// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-48323

// FILE: Jerry.java
public interface Jerry {
    void call(int i);
}

// FILE: Tom.kt
fun interface Tom {
    fun tom(i: Int)
}

fun foo(m: Tom) = 1
fun foo(j: Jerry) = "2"
fun test() {
    val result = <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>i<!> ->
        val j = i + 1
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: foo, [/foo, /foo]")!>result<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, funInterface, functionDeclaration, integerLiteral, interfaceDeclaration,
javaType, lambdaLiteral, localProperty, propertyDeclaration, samConversion, stringLiteral */
