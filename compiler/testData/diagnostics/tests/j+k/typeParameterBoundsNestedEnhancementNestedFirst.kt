// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-73257
// `Nested` is enhanced first: its second round enhances `Outer`, whose bounds are final when `Nested.U` is erased.
// The bound of `Nested.U` differs from typeParameterBoundsNestedEnhancementOuterFirst.kt
// DIAGNOSTICS: +DEBUG_INFO_EXPRESSION_TYPE
// FILE: Outer.java
public class Outer<X extends Nested> {}

// FILE: Nested.java
public class Nested<U extends Outer> {
    public U get() { return null; }
}

// FILE: main.kt
// Enhances `Nested` first
fun touchNested(n: Nested<*>) {}

fun test(n: Nested<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(Outer<(Nested<(Outer<*>..Outer<*>?)>..Nested<*>?)>..Outer<*>?)")!>n.get()<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType, starProjection */
