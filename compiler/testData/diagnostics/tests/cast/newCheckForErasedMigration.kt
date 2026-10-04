// RUN_PIPELINE_TILL: FRONTEND

interface Base<T>
interface Derived<T> : Base<T>
interface Marker

// The type parameter is used inside a composite argument in the supertype, so it is not inherited:
// the old unification-based algorithm can reconstruct it, while the new one cannot.
class Wrap<T> : Base<List<T>>

fun testBecameErased(x: Base<List<Int>>) {
    if (x is Wrap<Int>) {}
}

fun testStillNotErased(x: Base<Int>) {
    if (x is Derived<Int>) {}
}

fun testStillErased(x: Base<Int>) {
    if (x is Derived<String>) {}
}

fun testIntersection(x: Base<Int>) {
    if (x is Marker) {
        // The old algorithm cannot reconstruct static type information from an intersection type,
        // while the new one can.
        if (x is Derived<Int>) {}
    }
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration,
intersectionType, isExpression, nullableType, smartcast, typeParameter */
