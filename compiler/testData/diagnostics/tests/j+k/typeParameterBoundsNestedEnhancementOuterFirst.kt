// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-73257
// `Outer` is enhanced first: its second round enhances `Nested`, which reads the first-round bound of `Outer.X` (`Nested<*>`).
// The bound of `Nested.U` differs from typeParameterBoundsNestedEnhancementNestedFirst.kt
// and from the reversed resolution order, where `test` enhances `Nested` first
// DIAGNOSTICS: +DEBUG_INFO_EXPRESSION_TYPE
// FILE: Outer.java
public class Outer<X extends Nested> {}

// FILE: Nested.java
public class Nested<U extends Outer> {
    public U get() { return null; }
}

// FILE: main.kt
// Enhances `Outer` first
fun touchOuter(c: Outer<*>) {}

fun test(n: Nested<*>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("(Outer<(Nested<*>..Nested<*>?)>..Outer<*>?)")!>n.get()<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaType, starProjection */
