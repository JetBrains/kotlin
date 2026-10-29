// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90659
// OPT_IN: kotlin.contracts.ExperimentalContracts

// FILE: Base.kt
import kotlin.contracts.*

open class Base<T> {
    fun run(block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }
}

// FILE: JavaDerived.java
public class JavaDerived extends Base<Integer> {}

// FILE: main.kt
import kotlin.contracts.*

class Box<T> {
    fun run(block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }
}

class Derived : Base<Int>()

open class Mid<U> : Base<U>()

interface WithRun {
    fun run(block: () -> Int): Int
}

class Intersection : Base<Int>(), WithRun

class SuperCall : Base<Int>() {
    fun inPlaceLambdaViaSuperCall(block: () -> Int) {
        contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
        super.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
    }
}

class Members<T> {
    operator fun invoke(block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }

    fun (() -> T).call(): T {
        contract { callsInPlace(this@call, InvocationKind.EXACTLY_ONCE) }
        return this()
    }

    inline fun inlineRun(block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }
}

class Generic<T> {
    constructor(block: () -> T) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        block()
    }
}

typealias GenericAlias = Generic<Int>
typealias GenericAliasWithParameter<X> = Generic<X>

fun assignmentViaSubstitutedMember(box: Box<Int>) {
    val x: Int
    box.run { x = 1; 2 }
    x.inc()
}

fun inPlaceLambdaViaSubstitutedMember(box: Box<Int>, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    box.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaStarProjectedMember(box: Box<*>, block: () -> Nothing) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    box.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaContravariantMember(box: Box<in Int>, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    box.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun <T> inPlaceLambdaViaTypeParameterMember(box: Box<T>, block: () -> T) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    box.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaSafeCall(box: Box<Int>?, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(block, InvocationKind.AT_MOST_ONCE)<!> }
    box?.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaInheritedMember(derived: Derived, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    derived.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaNestedSubstitution(mid: Mid<Int>, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    mid.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaIntersectionOverride(intersection: Intersection, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    intersection.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaJavaSubclass(javaDerived: JavaDerived, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    javaDerived.run(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaInvoke(members: Members<Int>, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    members(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaExtensionReceiver(members: Members<Int>, block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    with(members) { <!LEAKED_IN_PLACE_LAMBDA!>block<!>.call() }
}

fun inPlaceLambdaViaInlineMember(members: Members<Int>, block: () -> Int) {
    contract { <!WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    members.inlineRun(block)
}

fun inPlaceLambdaViaGenericConstructor(block: () -> Int) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    Generic(block)
}

fun inPlaceLambdaViaGenericAlias(block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    GenericAlias(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

fun inPlaceLambdaViaGenericAliasWithParameter(block: () -> Int) {
    contract { <!LEAKED_IN_PLACE_LAMBDA, WRONG_INVOCATION_KIND!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!> }
    GenericAliasWithParameter(<!LEAKED_IN_PLACE_LAMBDA!>block<!>)
}

/* GENERATED_FIR_TAGS: assignment, capturedType, classDeclaration, contractCallsEffect, contracts, flexibleType,
funWithExtensionReceiver, functionDeclaration, functionalType, inProjection, inline, integerLiteral,
interfaceDeclaration, javaType, lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration, safeCall,
secondaryConstructor, starProjection, superExpression, thisExpression, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter */
