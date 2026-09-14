// LANGUAGE: +EliminateSecondKindIncorporation
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// DUMP_INFERENCE_LOGS: MARKDOWN

interface Box<T> {
    var x: T
}

fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun <Z : Collection<Any>> buildBoundedBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun <E1> mySetOf1(x: E1): Set<E1> = TODO()
fun <E2> mySetOf2(x: E2): Set<E2> = TODO()
fun <E3> myMutableSetOf1(x: E3): MutableSet<E3> = TODO()
fun <E4> myMutableSetOf2(x: E4): MutableSet<E4> = TODO()
fun <E7> myMutableSetOfLists1(x: E7): MutableSet<List<E7>> = TODO()
fun <E8> myMutableSetOfLists2(x: E8): MutableSet<List<E8>> = TODO()

interface Consumer<in T> {
    fun consume(t: T)
}

fun <E5> myConsumerOf1(x: E5): Consumer<E5> = TODO()
fun <E6> myConsumerOf2(x: E6): Consumer<E6> = TODO()

fun testWithSemifixation() {
    buildBox {
        x = mySetOf1("1")
        x = mySetOf2("2")
        x.size
    }.x.iterator().next().<!UNRESOLVED_REFERENCE("length;  on receiver of type 'Any?'")!>length<!> // Should work?
}

fun testWithUpperBound() {
    <!TYPE_MISMATCH("Collection<Any>; Set<*>")!><!UPPER_BOUND_VIOLATED("Collection<Any>; Set<*>; Z (of fun <Z : Collection<Any>> buildBoundedBox)")!>buildBoundedBox<!> {
        x = mySetOf1("1")
        x = mySetOf2("2")
        <!TYPE_MISMATCH("Collection<Any>; Set<*>")!>x<!>.size
    }<!>.x.iterator().next().<!UNRESOLVED_REFERENCE("length;  on receiver of type 'Any?'")!>length<!> // Z has a declared upper bound, which should not be preferred to the lower bounds
}

// TODO: review
// A flexible lower constraint next to a not-fixed variable: `Set<E1> <: Z`, `(MutableSet<T!>..Set<T!>?) <: Z`
fun testWithFlexibleType() {
    buildBox {
        x = mySetOf1("1")
        x = java.util.Collections.<!CANNOT_INFER_PARAMETER_TYPE("T")!>emptySet<!>()
        x.size
    }.x.iterator().next().<!UNRESOLVED_REFERENCE("length;  on receiver of type 'Any?'")!>length<!>
}

// TODO: review
// Invariant position: `MutableSet<E3> <: Z`, `MutableSet<E4> <: Z`
fun testInvariant() {
    buildBox {
        x = myMutableSetOf1("1")
        x = myMutableSetOf2("2")
        x.add(<!MEMBER_PROJECTED_OUT("MutableSet<*>; star; fun add(element: E): Boolean")!>"3"<!>)
    }.x.iterator().next().<!UNRESOLVED_REFERENCE("length;  on receiver of type 'Any?'")!>length<!>
}

// TODO: review
// The variables are nested in the invariant argument: `MutableSet<List<E7>> <: Z`, `MutableSet<List<E8>> <: Z`
fun testInvariantNested() {
    buildBox {
        x = myMutableSetOfLists1("1")
        x = myMutableSetOfLists2("2")
        x.add(listOf("3"))
    }.x.iterator().next().iterator().next().<!UNRESOLVED_REFERENCE("length;  on receiver of type 'Any?'")!>length<!>
}

// TODO: review
// Contravariant position: `Consumer<E5> <: Z`, `Consumer<E6> <: Z`. Not-fixed variables are ignored there as before,
// so Z is semi-fixed to `Consumer<*>` (see the inference log) and `consume` would not be callable
fun testContravariant() {
    buildBox {
        x = myConsumerOf1("1")
        x = myConsumerOf2("2")
        x.hashCode()
    }.x.hashCode()
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, propertyDeclaration, typeParameter, typeWithExtension */
