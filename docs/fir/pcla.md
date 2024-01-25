# Partially constrained lambda analysis

## Purpose
Partially constrained lambda analysis, PCLA for short, is a method for local type inference that allows
to infer types from usages, as opposed to the usual inference from the type expectation.

#### Usual type inference
```kotlin
fun <R> run(block: () -> R): R = block()

val a: String /* expectation */ = run<_ /* inferred */> {
    return@run materialize<_/* inferred */>() 
}

val b /* inferred */ = run<_ /* inferred */> {
    return@run "" /* type info source */
} 
```

#### Partially constrained lambda analysis
```kotlin 
interface Inv<T> {
    fun set(t: T)
    fun get(): T
}

fun <T> withInv(action: Inv<T>.() -> Unit): Inv<T> = createInv().action()

val c = withInv<_ /* String inferred */> { 
    set("str") // type info source
    
    Unit
}
```

The key difference between the `run` function and the `withInv` function is the type info flow direction.

While, in `run` function, type flow follows the code flow direction:
E.g.:
```kotlin
val a: String /* source */ = materialize() /* sink */
val b /* sink */ = "" /* source */
```

In the PCLA we have another form of the type flow, that couldn't be translated into the usual Kotlin code.

E.g.:
```kotlin
val _this: Inv<_ /* sink */> = createInv()
// -- withInv {
_this.set("str") /* source */
// -- withInv }
val c /* sink */ = _this
```

## Entry-point

See: `org.jetbrains.kotlin.fir.resolve.inference.ConstraintSystemCompleter.tryToCompleteWithPCLA`

Everything starts within the constraint system completion process.

At the moment, we already tried to infer type variables and analyzed lambdas that have enough type information to be analyzed.

Yet we still have left with the lambdas with not enough information.

#### Example
```kotlin
fun <A, B, C> twoSteps(first: Inv<A>.() -> Unit, b: B, second: (B) -> C): Triple<A, B, C> {
    val a = Inv<A>().apply(first)
    val c = second(value)
    return Triple(a, value, c)
}

fun main() {
    twoSteps(
        first = { set(1) },
        b = "b",
        second = { it }
    )
}
```

Here, during the call completion of the call to `twoSteps` function we would:
1. Fix `Bv <:> String`, from the `String <: Bv` constraint.
2. Analyze `second` lambda, since it has enough type info, `Bv <:> String` is known now.
    - `first` lambda still has `Av` in its input types: `/* input type */ Inv<Av>.() -> /* output type */ Unit`
    - `second` lambda input types are known: `(/* input type */ Bv=String) -> /*output type*/ Cv`
    - Get additional type constraint `Cv <: String` from the usage of it
3. Fix `Cv <:> String`, from the newly inferred lambda type info.
4. Realize, that the `first` lambda still doesn't have enough type info and its
   input types are still unknown.
5. tryToCompleteWithPCLA
    - Check if the `first` lambda type shape is suitable for PCLA
    - Analyze the `first` lambda in PCLA mode


To determine if the lambda is suitable for PCLA, we use the following condition:
- The lambda input types still contain non-fixed type variables.
- The non-fixed type variable is contained in the type arguments of an input type

Q!: What if there are two, such as: `(A<Av>, Bv) -> Unit` ?

Q!: Why we don't try to order it by readiness somehow? Such as that
`(Inv<Av>) -> Bv` is more ready then `(Inv<Av>, Bv) -> Unit` ?

N!: There are quite a lot of bugs that arise from probing of different lambda shapes.

## Preparation for root lambda analysis in PCLA

Q!: We seem to trigger PCLA mode analysis not only from `tryToCompleteWithPCLA`.
It looks like we can somehow trigger it accidentally if we meet the PCLA condition accidentally

Q!: Interaction with overload by lambda return type?

When we start lambda analysis, we check if the lambda satisfies the PCLA predicate and
initiate PCLA lambda analysis mode.

See: `org.jetbrains.kotlin.fir.resolve.inference.PostponedArgumentsAnalyzer.analyzeLambda`

Note: We shouldn't approximate lambda input types at this point. Otherwise, we would lose type variables that are used there.
```kotlin
fun <E> buildBox(build: (Box<E>) -> Unit) { /* ... */ }
fun main() {
    buildBox(
        build = { /* it: Box<TypeVariable(E)> !!! */ } // lambda atom = (Box<TypeVariable(E)>) -> Unit
    )
}
```

In this mode, inference session would be set to the PCLA inference session.

Here, we introduce:
- Outer candidate
  The chosen call candidate that directly or indirectly contains the PCLA lambda and is currently being completed.
- Outer constraint system
  The constraint system of the outer candidate.

Example:
```kotlin
fun <E> buildBox(build: (Box<E>) -> Unit) { /* ... */ }

fun main() {
    buildBox( // <- The outer candidate.
        build = { } // <-- The root PCLA lambda 
    )
    buildBox( // <- The outer candidate. !WRONG! Doesn't work
        build = id(
            { } // <-- The root PCLA lambda
        )
    )
}
```
Q!: Is there any way to create a situation when buildBox is an outer candidate for lambda that is deeply in the call tree?


During the creation of PCLA inference session, we create a constraint system.
`currentCommonSystem` set to the empty system with `outer` system set to the CS of the outer candidate.

### Adding the outer type system
Given, we had a type system of our outer candidate:
```
vars: [Av, Bv, Cv]
fixed: [Bv <:> String, Cv <:> String]
notFixed: Av
constraints: []
```

And an empty constraint system. The result would be:
```
vars: [Av, Bv, Cv]
fixed: [Bv <:> String, Cv <:> String]
notFixed: Av
constraints: []
outerSystemVariablesPrefixSize = 3
usesOuterCS = true
outerCS = CS of the outer candidate
```

Q!: Why we don't just use postponed variables and avoid all the complexity with the outer system?


## Analysis of lambda body under the PCLA inference session

During the analysis of lambda body, we keep track of call resolution, so that during the candidate creation we:
- Decide if the candidate can add some type info to PCLA
    - See: `org.jetbrains.kotlin.fir.resolve.inference.FirPCLAInferenceSession#needToBePostponed`
    - See: `org.jetbrains.kotlin.fir.resolve.inference.FirPCLAInferenceSession#baseConstraintStorageForCandidate`
- Use the outer constraint system as a base system for such candidates.
- Avoid joining argument systems if it is already accounted for in the outer system.
    - Q!: (How is it accounted for?)

Q!: What about the fact that we have different candidate factories?
What happens if we mix argument positioning, such as some random receivers being added to the constraints
TODO: Example

Q!: What is the deciding factor for needToBePostponed?

Once a candidate for call is selected, we run call completion.
If the context allows for FULL completion, we allow it only if the candidate doesn't use the outer system.
Otherwise, we use special completion mode `PCLA_POSTPONED_CALL`.
We allow call to be completed in the `PARTIAL` mode anyway.

Q!: What are the consequences?

This completion mode is similar to `PARTIAL`, so it doesn't invoke completion writing and avoid call finalization.

Q!: Interaction between the completion modes?

E.g.:
```kotlin
buildList {
    add("str") // <-- This call can be completed with FULL completion as it has not type parameters, but we want
    // to use the type information from this call in the PCLA of the build function. 
}
```

PCLA_POSTPONED_CALL and nested lambda analysis
TODO: What is it?

After completion of any call either in `PCLA_POSTPONED_CALL` or `PARTIAL` mode, we need to re-integrate its system back
into the current constraint storage.

TODO: Fix for member scope?

If the call uses the outer constraint system, we replace current constraint storage with its constraints.
It is needed to account for the new constraints on the outer type variables that emerged from the call.

E.g.:
```kotlin
buildList { // (0), tv = E 
    add( // (1)
        id("str") // (2)
    )
    Unit
}
```

The analysis order will be:
- Start PCLA for lambda 0
- Select candidate for `id (2)` call
    - Base system will be empty, since `id` doesn't need to be postponed.
- Complete `id (2)` in FULL mode. Outer CS isn't used in the id call.
- Select candidate for `add (1)` call
    - Base system will use the outer system with type variable `E`
- Complete `add (1)` in the PCLA_POSTPONED_CALL mode instead of FULL, as it uses outer CS.
- Replace the current constraint system of the inference session with the CS of `add (1)` call candidate.
    - The `add (1)` system brings additional constraint: `String <: E`
-



Q!: What if no constraint system was initialized for the candidate?

See: `org.jetbrains.kotlin.fir.resolve.inference.FirPCLAInferenceSession.processPartiallyResolvedCall`



Once the lambda body is analyzed, we return to the completion of the outer call.
Before doing so, we need to



# High-level algorithm description


Let `CS` be the constraint system currently being completed.

1. Try to fix type variables as usual
2. If there are no new type info sources left while some lambdas are not analyzed yet. Try PCLA.
3. For all lambdas that conform to:
    - Any of the input types contains non-fixed type variable in its type arguments.
4. Create a constraint system `OCS` with:
    - Copy of the `CS`
    - Mark all non-fixed type variables of `CS` as `outer`
5. Perform lambda analysis with special call handling
6. For every call in the lambda body:
7. During the call resolution:
    1. Construct the common constraint system `B` for the candidate factory
        - Ignore any arguments that reference `OCS`
    2. Determine if the call candidate can reference any type variable type of type variable that is `outer`
    3. For such a call candidate, use the `OCS` constraint system as the base constraint system.
8. If the call uses `OCS`, override `FULL` completion mode with the `PCLA_POSTPONED_CALL`
9. Complete call
    1. When we complete the call in PCLA_POSTPONED_CALL mode:
        - Try to fix variables with enough type information that doesn't have a relation with outer type variables.
        - Analyze ready lambdas
        - Try to start nested PCLA analysis
        - Force analysis of remaining lambdas if there is still not enough type info without fixing its input types.
          - Q!: What about the ordering change
          - Q!: Why we treat such input types as not outer type variables
    2. When we analyze lambdas that aren't subject to nested PCLA, we use the constraint system of the lambda-containing call tree as the `OCS`
    3. When we analyze a lambda that needs nested PCLA, what will happen?
10. If the call expression is used in the receiver position, and its type is a type variable type on the top-level, perform semi-fixing:
    - Compute a result type for that type variable, treating outer type variables as proper types
    - Add equality constraint Tv <:> RT
    - Substitute Tv with RT in the expression type
11. Substitute all fixed type variables in the return type of candidate
12. If the call uses `OCS`, replace `OCS` content with CS of the candidate
13. If the call was completed in the `PCLA_POSTPONED_CALL` store it in `postponedPCLACalls`
14. Once lambda body is analyzed:
15. Replace `CS` content with `OCS`.
    - Note that this operation doesn't update the `outer` marking of type variables.
16. Repeat for all lambdas that conform to the initial condition
17. Continue the outer call completion.
18. During the completion writing we perform additional traversal of `postponedPCLACalls` with completion results writer.
19. Apply final substitutor of `CS` to all lambda bodies that were analyzed with PCLA to get rid of references to type variables everywhere.

``` 
...
if there are no new type info sources left while some lambdas are not analyzed yet:
    for lambda in postponed:
        if any of the input types contains non-fixed type variable in its type arguments:
            apply pcla to lambda: 
                let OCS = copy(CS)
                OCS.outerVariables = CS.allTypeVariables
                before call resolution:
                    // Base system for candidate factory
                    let B = empty CS + systems of arguments that doesn't reference OCS
                for each candidate: 
                    if call can somehow reference type variable from OCS:
                        candidate.system = OCS + B
```


# Notes
- PCLA variables as special in the sense that they represent a different form of type flow info
- Maybe we should handle it as something special?
- What if we oppose the variables found in input types to the set of all variables in the
  pcla host call

- What about the ordering in the tryToComplete with the builder inference?
- What about trying to fix some variables between the tryToCompleteWithPCLA?

- PCLA lambda shape condition is still sketchy

Q!: Interaction with PostponedAtomWithRevisedExpectedType

- Construction of the base system happens multiple times per call site that should be resolved.

- interaction with overload resolution by lambda return type

- getOrderedAllTypeVariables seems strange

- Why we use ConeFixVariableConstraintPosition -- it has a non-trivial consequences

replaceContentWith is strange; it's not what it named as
- Updates not whole state (Ignores outerTypeVariables)
- Combines initial constraints
- Duplicates error list
- Duplicates fork point constraints


Idea: Consolidate system handling