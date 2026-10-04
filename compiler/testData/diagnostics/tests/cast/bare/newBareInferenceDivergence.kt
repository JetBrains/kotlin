// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface Base<T>
interface Derived<T> : Base<T>

// The type parameter of `Reversed` is not inherited from `Base`,
// so the new bare inference algorithm infers `Reversed<*>`,
// while the old one unifies `Reversed<T> :> Base<String>` to `Reversed<String>`.
interface Reversed<T> : Base<T>

class Container<A, B> : Base<B>

fun testInherited(b: Base<String>) {
    if (b is Derived) {
        // The type parameter is inherited, both algorithms infer Derived<String>.
        takeDerivedOfString(b)
    }
}

fun testNotInherited(c: Base<String>) {
    if (c is Container) {
        takeContainer(c)
    }
}

fun takeDerivedOfString(d: Derived<String>) {}
fun takeContainer(c: Container<*, String>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nullableType, smartcast, starProjection, typeParameter */
