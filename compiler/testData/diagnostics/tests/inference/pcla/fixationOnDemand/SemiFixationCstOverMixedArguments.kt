// LANGUAGE: +EliminateSecondKindIncorporation +SyntheticTypeVariablesForCstOverTypeVariables
// WITH_STDLIB
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84797, KT-88681
// RENDER_DIAGNOSTIC_ARGUMENTS
// DUMP_INFERENCE_LOGS: MARKDOWN

interface I {
    fun foo()
}

class A : I {
    override fun foo() {}
}

class B : I {
    override fun foo() {}
}

interface Box<T> {
    var x: T
}

fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z> = TODO()

fun <E1> mySetOf1(x: E1): Set<E1> = TODO()
fun <E2> mySetOf2(x: E2): Set<E2> = TODO()
fun <E3> myMutableSetOf(x: E3): MutableSet<E3> = TODO()
fun <E5> myMutableSetOf2(x: E5): MutableSet<E5> = TODO()
fun <E6> myMutableSetOfLists(x: E6): MutableSet<List<E6>> = TODO()
fun <E7> myMutableSetOfOut(x: E7): MutableSet<out E7> = TODO()
fun <E8> myMutableSetOfOutLists(x: E8): MutableSet<out List<E8>> = TODO()

interface Consumer<in T> {
    fun consume(t: T)
}

fun <E4> myConsumerOf(x: E4): Consumer<E4> = TODO()

// Covariant position: `Set<E1> <: Z` with E1 not fixed and the proper `Set<B> <: Z`
fun testVariableFirst(setOfB: Set<B>) {
    <!TYPE_MISMATCH("B; A")!>buildBox {
        x = <!ASSIGNMENT_TYPE_MISMATCH("Set<B>; Set<A>")!>mySetOf1<!>(A())
        x = setOfB
        <!TYPE_MISMATCH("B; A")!>x<!>.size
    }<!>.x.iterator().next().foo()
}

fun testProperFirst(setOfB: Set<B>) {
    <!TYPE_MISMATCH("B; A")!>buildBox {
        x = setOfB
        x = <!ASSIGNMENT_TYPE_MISMATCH("Set<B>; Set<A>")!>mySetOf1<!>(A())
        <!TYPE_MISMATCH("B; A")!>x<!>.size
    }<!>.x.iterator().next().foo()
}

fun testTwoProperOneVariable(setOfA: Set<A>, setOfB: Set<B>) {
    buildBox {
        x = setOfA
        x = mySetOf2(B())
        x = setOfB
        x.size
    }.x.iterator().next().foo()
}

fun testStringAndInt() {
    val mySetOfInt = mySetOf1(2)
    val box = <!TYPE_MISMATCH("Int; String")!>buildBox {
        x = <!ASSIGNMENT_TYPE_MISMATCH("Set<Int>; Set<String>")!>mySetOf2<!>("1")
        x = mySetOfInt
        <!TYPE_MISMATCH("Int; String")!>x<!>.size
    }<!>
    val probe: Comparable<*> = box.x.iterator().next()
}

// The reporter's case
interface NullableBox<B> {
    var b: B?
}

fun <B> buildNullableBox(block: NullableBox<B>.() -> Unit): NullableBox<B> = TODO()

fun testKt84797() {
    val setInt: Set<Int> = setOf(42)
    <!TYPE_MISMATCH("Int; String")!>buildNullableBox {
        b = setInt
        b = <!ASSIGNMENT_TYPE_MISMATCH("Set<Int>?; Set<String>")!>setOf<!>("42")
        <!TYPE_MISMATCH("Int; String")!>b<!>?.size
    }<!>
}

// Invariant position: the proper argument still determines the semi-fixation result,
// so the element variable is equated to it
fun testInvariant(mutableSetOfB: MutableSet<B>) {
    buildBox {
        x = myMutableSetOf(B())
        x = mutableSetOfB
        x.add(B())
    }.x.add(B())
}

// The same position with different element types: the mismatch between A and B is reported
fun testInvariantMismatch(mutableSetOfB: MutableSet<B>) {
    <!TYPE_MISMATCH("B; A")!>buildBox {
        x = myMutableSetOf(A())
        x = mutableSetOfB
        <!TYPE_MISMATCH("B; A")!>x<!>.add(<!ARGUMENT_TYPE_MISMATCH("B; B")!>B()<!>)
    }<!>
}

// TODO: review
// Two not-fixed variables next to the proper argument, in either order: `MutableSet<E3> <: Z`, `MutableSet<E5> <: Z`,
// `MutableSet<B> <: Z`. The proper argument determines the result, `MutableSet<B>`
fun testInvariantTwoVariables(mutableSetOfB: MutableSet<B>) {
    buildBox {
        x = myMutableSetOf(B())
        x = myMutableSetOf2(B())
        x = mutableSetOfB
        x.add(B())
    }.x.add(B())
}

fun testInvariantTwoVariablesProperFirst(mutableSetOfB: MutableSet<B>) {
    buildBox {
        x = mutableSetOfB
        x = myMutableSetOf(B())
        x = myMutableSetOf2(B())
        x.add(B())
    }.x.add(B())
}

// TODO: review
// The variable is nested in the invariant argument: `MutableSet<List<E6>> <: Z`, `MutableSet<List<B>> <: Z` gives
// `MutableSet<List<B>>`
fun testInvariantNested(mutableSetOfListsOfB: MutableSet<List<B>>) {
    buildBox {
        x = myMutableSetOfLists(B())
        x = mutableSetOfListsOfB
        x.add(listOf(B()))
    }.x.add(listOf(B()))
}

// TODO: review
// An out-projected argument next to the proper one: `MutableSet<out E7> <: Z`, `MutableSet<B> <: Z`. The result is
// out-projected in any case, so the position is treated like a covariant one and the variable joins the common supertype
fun testInvariantOutProjected(mutableSetOfB: MutableSet<B>) {
    <!TYPE_MISMATCH("B; A")!>buildBox {
        x = <!ASSIGNMENT_TYPE_MISMATCH("MutableSet<out B>; MutableSet<out A>")!>myMutableSetOfOut<!>(A())
        x = mutableSetOfB
        <!TYPE_MISMATCH("B; A")!>x<!>.size
    }<!>.x.iterator().next().foo()
}

// TODO: review
// The same with the variable nested in the out-projected argument: `MutableSet<out List<E8>> <: Z`, `MutableSet<List<B>> <: Z`
fun testInvariantOutProjectedNested(mutableSetOfListsOfB: MutableSet<List<B>>) {
    <!TYPE_MISMATCH("B; A")!>buildBox {
        x = <!ASSIGNMENT_TYPE_MISMATCH("MutableSet<out List<B>>; MutableSet<out List<A>>")!>myMutableSetOfOutLists<!>(A())
        x = mutableSetOfListsOfB
        <!TYPE_MISMATCH("B; A")!>x<!>.size
    }<!>.x.iterator().next().iterator().next().foo()
}

// TODO: review
// Contravariant position: `Consumer<E4> <: Z` with E4 not fixed and the proper `Consumer<B> <: Z`
fun testContravariant(consumerOfB: Consumer<B>) {
    buildBox {
        x = myConsumerOf(A())
        x = consumerOfB
        x.consume(B())
    }.x.consume(B())
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, integerLiteral,
interfaceDeclaration, lambdaLiteral, localProperty, nullableType, override, propertyDeclaration, safeCall,
starProjection, stringLiteral, typeParameter, typeWithExtension */
