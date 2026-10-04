// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

interface Base<T>

// The variances of the type parameters differ: the new algorithm wraps the propagated argument
// into a use-site projection, while the old one infers an invariant argument.
interface CovBase<out T>
interface InvFromCov<T> : CovBase<T>

// The bounds differ, so the type parameter is not inherited according to the new algorithm.
interface Bounded<T : Number> : Base<T>

fun takeInv(i: InvFromCov<String>) {}
fun takeBounded(b: Bounded<Int>) {}

fun testVarianceDivergence(c: CovBase<String>) {
    if (c is InvFromCov) {
        // Old inference: InvFromCov<String>; new inference: InvFromCov<out String>.
        takeInv(c)
    }
}

fun testBoundsDivergence(b: Base<Int>) {
    if (b is Bounded) {
        // Old inference: Bounded<Int>; new inference: Bounded<*>.
        takeBounded(b)
    }
}

fun testMutableList(l: List<String>) {
    if (l is MutableList) {
        // Old inference: MutableList<String>; new inference: MutableList<out String>.
        l.add("")
        l.size
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
nullableType, out, smartcast, typeConstraint, typeParameter */
