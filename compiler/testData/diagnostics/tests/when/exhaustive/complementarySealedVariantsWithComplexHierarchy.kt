// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-87189
// LANGUAGE: +DataFlowBasedExhaustiveness
// WITH_STDLIB

sealed interface A
interface B : A
sealed interface C : A
sealed interface H : A

sealed interface D : B
interface E : D
sealed interface F : D

data object G : C

data object I : H, F

fun check(a: A) {
    require(a is D || a is H)

    <!NO_ELSE_IN_WHEN!>when<!> (a) {
        is E -> println("E")
        is F -> println("F")

        // Technically, impossible, but `CST(D, H) = A` and there's no way to express this in `TypeStatement`.
        // Theoretically, implications like `(lhsVar eq !saturatingValue) implies approvedRightStatements` could
        // work, but `a is H` in `require()` and `is H` in a later `when` branch will have two distinct DFA variables,
        // so these will be two different `OperationStatement`s, and the latter being True won't approve the former.
        // Plus, there's currently no logic that would conclude (`a !is E` and `a !is F`) implies `a !is D` in
        // `LogicSystem.kt`.
        is B -> println("B")

        is H -> println("H")
        // We definitely know that neither `G`, nor `C` are possible here.
    }
}

fun checkTraversalAcrossParallelSealedPath(a: A) {
    when (a) {
        is D -> <!NO_ELSE_IN_WHEN!>when<!> (a) {
            is E -> {}
            // Must be `is I`
        }
        else -> {}
    }
}

/* GENERATED_FIR_TAGS: data, disjunctionExpression, functionDeclaration, interfaceDeclaration, isExpression,
objectDeclaration, sealed, smartcast, stringLiteral, whenExpression, whenWithSubject */
