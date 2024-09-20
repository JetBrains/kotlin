# Partially Constrained Lambda Analysis

**Prerequisites:** That document is built under the assumption that the reader is more or less aware of how inference
works for calls without lambdas or with lambdas for which all input-types are proper at the point where
they should be analyzed. Also, some terms that are used here described at [Common inference terms definition](inference.md)

See also: [Kotlin Spec: Builder-style type inference](https://kotlinlang.org/spec/type-inference.html#builder-style-type-inference)

The most basic scenario where PCLA is used is calls to builder-like functions:

```kotlin
fun <E> buildList(builderAction: MutableList<E>.() -> Unit): List<E> = ...

fun main() {
    buildList { 
        add("")
    } // E is inferred to `String`
}
```

There in the `buildList` call there is no other source of information what type should be inferred for `E` beside the body of the lambda.
That is the case where partially constrained lambda analysis (PCLA) starts.

## Glossary

**Regular lambda analysis**

Currently, we start lambda analysis only during call completion (`ConstraintSystemCompleter.runCompletion`).
Let's call "regular lambda analysis" situation when we start body resolution of the anonymous function when all input types do not contain
uninferred TVs.

**PCLA (Partially Constrained Lambda Analysis)**

PCLA is the way of lambda analysis during call completion that happens when some input types are not properly inferred, and there are no other sources of constraints.

**PCLA lambda** = lambda analyzed with PCLA

**Main call/candidate** is the call that contains PCLA lambda

**Nested call/candidate** is a call being resolved in other than `ContextDependent` mode (usually statement-level) that belongs to PCLA lambda
  * **Postponed nested calls** are nested calls which we decided not to complete in the FULL mode immediately

**Shared CS** is a constraint system being shared between postponed nested calls inside PCLA lambda

**Outer CS**
  * Defined only for nested postponed candidates (for shared CS)
  * It consists of type variables of the containing PCLA lambdas and all the type variables of already processed inner candidates
  * For inner CS, all the outer CS-related type variables are always coming at the beginning of the list of all variables

**Outer Type Variable (TV)**
  * In the context of *shared CS*, it's a subset of type variables that are brought from the *outer CS*
  * In the list of all variables (`NewConstraintSystemImpl.allTypeVariables`) outer TV always comes in the beginning.
    * See `NewConstraintSystemImpl.outerSystemVariablesPrefixSize`

## Entry-point to PCLA

The conditions to run a lambda resolution in PCLA mode is the following:
- Completion mode for the current call tree is `FULL` 
- There are no other ways to infer some new constraints for any type variable.
- There is some lambda that among its input types has at least one with a not fixed TV being used as type argument
    - So, if one of the input types is `MutableList<Ev>` that lambda suits for PCLA
    - But if some of the input types are proper and other are top-level type variables (`Tv`, `Tv?`, `Tv & Any`, etc.), it doesn't suit

**References in code:**
- `ConstraintSystemCompleter.tryToCompleteWithPCLA`

During PCLA, there is a quite specific behavior of how actually lambda body resolution
happens, and describing it is actually a goal of the following parts document.

## Algorithm backbone

The basic idea is that we mostly run regular lambda body analysis, with special treatment for all calls inside. Namely,
- They use **shared CS**: adding their new variables there and constraints related to both *outer CS* and their own variables
- They are not being fully completed (leaving them postponed)

After lambda's body traversal, the whole **shared CS** is added to the CS of the *main candidate*, the resulting CS should be resolved 
as usual, and after that at completion results writing phase all type variables are replaced with their result types.

### More details

* **NB:** This algorithm doesn't use any stub types, just regular type variables everywhere
* Once we see a need to start PCLA (see [Entry-point to PCLA](#entry-point-to-pcla)), we create
    * `FirPCLAInferenceSession` (introduced at `LambdaAnalyzerImpl.analyzeAndGetLambdaReturnArguments`)
    * Shared CS (see `FirPCLAInferenceSession.currentCommonSystem` property)
      * It's mostly a copy of **outer CS** (the CS of the containing candidate), but we mark all the variables as *outer*
      * See `FirInferenceSession.Companion.prepareSharedBaseSystem`
* After that, we run analysis inside the lambda
    * For nested candidates that are really trivial (no receivers with TV, no lambdas, no value arguments using outer CS), we just regularly complete them
    * For other calls
        * Instead of empty initial CS, we supply a **shared CS**
        * So, before introducing its own TVs, CS already contains of outer TV
        * Run candidate resolution within this CS
        * Do not run full completion, just gather all the constraints and variable back to the **shared CS**

* In the end, the **root CS** (aka CS for the Main candidate) contains TVs for all incomplete nested candidates.
* And may just continue `FULL` completion process on the Main, i.e., fixing variables for which we now have new proper constraints, 
  and analyze other lambdas.
* In the end, beside completion results writing for the main call, we also write results (inferred type arguments/expression type update) 
  for all incomplete/postponed calls (see `FirCallCompletionResultsWriterTransformer.runPCLARelatedTasksForCandidate`).

### Example 1

```
fun foo() {}

fun main() {
    val x = listOf("")
    buildList /* Main candidate/call */ {
        foo() // Irrelevant call that doesn't refer PCLA variables, might be fully completed
        
        // Uses shared CS
        // Adds String <: Ev constraint
        // Pushes all constraints back to the shared CS
        add("") // Postponed nested call
        
        // Uses shared CS
        // Candidate declaration: fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapTo(destination: C, transform: (T) -> R): C
        // Adds Tv, Rv, Cv type variables 
        // And constraints
        // String <: Tv [from List<String> <: Iterable<Tv>]
        // MutableList<Ev> <: Cv
        // Rv <: Ev [incorporation from MutableList<Ev> <: MutableCollection<in Rv> 
        // Fixing Tv := String
        // Starting lamda analysis
        // Having 
        // Rv <: String constraint
        // 
        // Then add all the new constraints to the shared CS
        x.mapTo(this) { it } // Postponed nested call
    }
 
    // And start looking for CS solution that would be sound
    // Ev := String
    // Rv := String
    // Cv := MutableList<String>
```

### Example 2
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

### Shared CS definition

- It's been initialized at `FirPCLAInferenceSession.currentCommonSystem`.
- Initially, it's an empty CS with the main candidate's system being added as an outer CS.

## Inference session callbacks

Currently, there are three implementations of inference session
- `Default` that effectively does nothing
- `FirPCLAInferenceSession` (being used during PCLA lambda resolution)
- `FirDelegatedPropertyInferenceSession` (being used for delegated operator resolution)

### baseConstraintStorageForCandidate

It returns *shared CS* if the candidate is considered applicable (non-trivial/postponed nested).

That callback is supposed to be called for each created candidate of all nested calls, and the 
content of returned CS should be integrated into the candidate CS.

See `org.jetbrains.kotlin.fir.resolve.calls.Candidate.getSystem`.

The shared CS is not supposed to be modified during candidate resolution 
(because there might be more than one of them for each call, while only a single one should be chosen).

### customCompletionModeInsteadOfFull

Being called after the single successful candidate is chosen just before the completion begins.

Currently, it returns `PCLA_POSTPONED_CALL` if the candidate is *postponed*.

See more details on `PCLA_POSTPONED_CALL` completion mode in the [section](#pclapostponedcall-completion-mode) below.

### processPartiallyResolvedCall

This callback is assumed to be called after `PARTIAL` or `PCLA_POSTPONED_CALL` completion terminates.

Mostly, it collects all *postponed* candidates and integrates their CS content into the shared CS.

Also, here we substitute the type of that given call expression with substitutor replacing already fixed type variables
with their result types.
Otherwise, using fixed TVs as expression types further might lead to `TypeCheckerStateForConstraintInjector.fixedTypeVariable` 
throwing an exception.

```kotlin
fun <S> id(e: S) = e

fun main() {
  buildList {
    // `id(this.size) ` type is `Sv`.
    // But we actually fixed `Sv` to Int because it's resolved in independent context and `Sv` is not related
    // to the outer CS.
    //
    // But since we don't run completion writing results, no one would replace the expression type of the call
    // with new resulting type.
    // So, we do it at `processPartiallyResolvedCall`
    val x = id(this.size)

    add(x)
  }
}
```

Note that this situation with fixed TVs can't happen outside PCLA context because
- For partial completion, we avoid fixing TVs that are used inside return types
- For FULL completion, we would run completion results writing, so there would be no candidates and type variables inside return types.

And the last part is fixing TVs for receiver (see the relevant [section](#on-demand-variable-fixation) for details).

### runLambdaCompletion

This part is rather controversial and hacky, hope we could get rid of it at some point.

When resolving a lambda at some nested call inside PCLA lambda, like

```kotlin
buildList { 
    myOtherCallWithLambda {
        add("") 
        Unit
    }
}
```

After completion for `add("")` call, we haven't yet called `processPartiallyResolvedCall` for `myOtherCallWithLambda { ... }`, 
because it's not yet completed, and we are already going to call `processPartiallyResolvedCall` for `add("")`, that would contribute some
constraints to `buildList` type variable `Ev` in shared CS.

And after that we finally call `processPartiallyResolvedCall` for `myOtherCallWithLambda { ... }`, but it doesn't contain constraints 
on `Ev` from `add(""")`, thus overwriting existing information.

That might be workarounded with some kind of merging constraints for the same TV instead of just replacement 
(see `NewConstraintSystemImpl.addOtherSystem`), but that seems to be hacky too.

So, the current approach is the following: during nested lambda resolution we assume that currently *shared CS* is equal to the CS of the
`myOtherCallWithLambda` candidate. Thus, the constraints from `add` call are being merged into the `myOtherCallWithLambda`'s CS and then 
finally into regular shared CS.

**Hacky place**: the proper solution might be just replacing instance of shared CS with current candidate's CS, but that needs 
some further investigation.

Also, there's some special handling of overload resolution by lambda return type, but they are quite expected when we don't have a final
candidate yet (see `analyzeLambdaAndReduceNumberOfCandidatesRegardingOverloadResolutionByLambdaReturnType`)

### runCallableReferenceResolution

TODO: Mostly, the idea as the same as for lambdas, but for callable references

### addSubtypeConstraintIfCompatible

In some situations, some constraints might be originated not just from calls, but from other kinds of expression, 
like from variable assignments:

```kotlin
var <F> MutableList<F>.firstElement 
    get() = get(0)
    set(f: F) {
        set(0, f)
    }

fun main() {
    buildList { 
        firstElement = ""
    }
}
```

We don't specially resolve the call to the setter, thus to declare that `String <: Fv`, thus `String <: Ev`, and finally `Ev := String`,
there's a need to somehow send this information to the inference session.
And that's how `addSubtypeConstraintIfCompatible` might be used.

**Hacky place**: In general, this approach is quite fragile, and we might've forgotten some places where this method should be triggered.
One of the ideas particularly for assignment is that they should be resolved via setter call, thus the necessary constraint would be
introduced naturally when string literal would be an argument for `Fv` value parameter.

### getAndSemiFixCurrentResultIfTypeVariable

Before deep-diving into this section, it's worth reading [On demand variable fixation](#on-demand-variable-fixation) section.

Sometimes, besides computing member scope, there might be other cases when we need to fix a type variable on-demand.

```kotlin
interface A<F> 
interface B<G> : A<G>

fun <X> predicate(x: X, c: MutableList<in X>, p: (X) -> Boolean) {}

fun main(a: A<*>) {
    buildList { 
        predicate(a, this) {
            it is B
        }
    }
}
```

In this example, for `is` check, `B` type on the right-hand side is a bare type and to compute its arguments properly, we need to know
the proper type representation of `it` which is not proper yet (`Xv` variable).

Potentially, we might've ignored that requiring full type arguments for `B`, but that would be a breaking change from a user project
([KT-64840](https://youtrack.jetbrains.com/issue/KT-64840)), so we decided to fix the type variable to the current result type.

That's how this callback is currently used from the place before bare-type computation is started.

## PCLA_POSTPONED_CALL completion mode

This mode is assumed to be used for postponed nested calls inside PCLA lambdas instead of FULL mode (i.e., mostly for top-level calls).

The set of the type variables that might be considered for fixation are obtained from the call tree of the supplied nested call itself,
i.e., it would be a set of fresh type variables of the candidate itself, type variable of its arguments and from the return statements
of lambdas. That part works the same way as for regular calls outside PCLA context.

### Example
```
fun foo(x: List<String>) {
    buildList {
        x.mapTo(this) { // `mapTo` analyzed in PCLA_POSTPONED_CALL
            add("") // analyzed in PCLA_POSTPONED_CALL
            it.length // analyzed in PCLA_POSTPONED_CALL
        }
        
        println("") // analyzed in FULL mode (because it's a trivial call, irrelevant to outer CS)
        
        add(x.get(0) /* analyzed in PARTIAL */) // analyzed in PCLA_POSTPONED_CALL
    }
}
```

### Short description

- Type variables are gathered from the call tree of the supplied *nested* call.
- Variable fixation is not allowed if the TV is *deeply related to any TV from the outer CS*.
- Analysis of all lambdas belonging to the supplied nested call *is forced even if some input types are not properly inferred*.
- We don't run completion writing in this mode.

### Limitation on variable fixation

The `PCLA_POSTPONED_CALL` mode imposes the following limitation on variable fixation:
it is not allowed to fix TVs that are deeply related to any TV from the outer CS
(see [inference.md](inference.md#related-tvs) for the definition of a deep relation).

**Source code reference:** `TypeVariableFixationReadiness.OUTER_TYPE_VARIABLE_DEPENDENCY`

Consider the following example:

```kotlin
class Container<T> {
    fun consume(t: T) {}
}

fun <X> Container<X>.consumeAll(xs: List<X>) {}

fun <OT> pcla(func: (Container<OT>) -> Unit): OT = TODO()

interface Parent
object ChildA : Parent
object ChildB : Parent

fun foo(childrenB: List<ChildB>) {
    pcla { container ->
        container.consumeAll(childrenB)
        // candidate CS:
        //     Controller<OTv> <: Controller<Xv>  =>  OTv == Xv
        //     List<ChildB> <: List<Xv>  =>  ChildB <: Xv
        // let Xv := ChildB; then:
        //     OTv == Xv  =>  OTv == ChildB
        
        container.consume(ChildA)
        // candidate CS:
        //     ChildA <: OTv
        
        // the resulting shared CS:
        //     OTv == ChildB
        //     ChildA <: OTv
        // is contradictory as ChildA </: ChildB
    }
}
```

However, `OTv` could be correctly inferred to `Parent` if `Xv` was not immediately fixed to `ChildB`:

```kotlin
        container.consumeAll(childrenB)
        // candidate CS:
        //     Controller<OTv> <: Controller<Xv>  =>  OTv == Xv
        //     List<ChildB> <: List<Xv>  =>  ChildB <: Xv
        
        container.consume(ChildA)
        // candidate CS:
        //     ChildA <: OTv
        
        // the resulting shared CS:
        //     OTv == Xv
        //     ChildB <: Xv
        //     ChildA <: OTv
        // can be solved via:
        //     Xv := OTv
        //     OTv := CST(ChildA, ChildB) == Parent
```

So we postpone fixation of `Xv` as deeply related to an outer TV to achieve this result.

### Forcing lambda analysis

Unlike the `FULL` mode, `PCLA_POSTPONED_CALL` doesn't require fixing all the variables;
it works like the `PARTIAL` mode in this sense.

But unlike `PARTIAL`, `PCLA_POSTPONED_CALL` forces analysis of all lambdas even if some input types are not properly inferred.
It's crucial because gained contract-affecting information would otherwise stop working across different statements.

As a result, three kinds of scenarios are possible when completing postponed nested calls with lambdas:
1. Nested PCLA scenario:
   - [for some lambdas, some input types contain not-fixed type variables as type arguments](#entry-point-to-pcla)
2. Top-level variable scenario (the most complicated):
   - for all lambdas, no input types contain not-fixed type variables as type arguments
   - but for some lambdas, some input types are top-level not-fixed type variables
3. Trivial scenario:
   - for all lambdas, no input types contain not-fixed type variables as type arguments
   - and for all lambdas, no input types are top-level not-fixed type variables

In the nested PCLA scenario, relevant lambdas are generally processed via [nested PCLA](#nested-pcla).

In the top-level variable scenario (and sometimes in the nested PCLA scenario),
relevant lambdas are processed via regular lambda analysis with the following modifications:
- If a top-level not-fixed type variable is a lambda parameter's type, we don't do anything special.
- But if a top-level not-fixed type variable is a **lambda receiver**'s type, we [fix it on demand](#on-demand-variable-fixation).
  - (the alternative is to leave the TV as is, the same as in lambda parameters' case;
    but it seems meaningless as almost any call inside the lambda
    might require the member scope of the lambda receiver,
    which in turn requires fixing the TV anyway)

In the trivial scenario, all lambdas are processed via regular lambda analysis.

## Analysis mode for return statements of a PCLA lambda

In most of the use cases we know for PCLA, the return type of the lambda is Unit, so all the return statements are being analyzed in a 
FULL (to be precise [PCLA_POSTPONED_CALL](#pcla_postponed_call-completion-mode)).

But there are some of them 
(e.g., [KT-68940](https://youtrack.jetbrains.com/issue/KT-68940/K2-IllegalArgumentException-All-variables-should-be-fixed-to-something)) 
where the return type might contain not-fixed type variables.

For **non**-PCLA lambdas using expected types with non-fixed type variables would lead to illegal state: 
calls inside return statements are not aware of type variables of the containing call.

But for PCLA, we resolve everything within a common CS; thus it's ok. 
Moreover, in some situations which look quite reasonable, it's even preferable to force e.g. lambda analysis in the return statements
to gather more constraints for the builder type variables.

```kotlin
interface Base
class Derived1 : Base
class Derived2 : Base

fun <E> myBuildSet(transformer: (MutableSet<E>) -> MutableSet<E>): MutableSet<E> = TODO()

fun main() {
    myBuildSet { builder ->
        builder.add(Derived1())

        // Return-statement of the lambda
        // Expected type: MutableSet<Ev> has a not-fixed type variable
        builder.let {
            it.add(Derived2()) // Should be OK

            it
        }
    }
}
```

By default, (e.g., in non-PCLA context) we would postpone the lambda in the `let` call, and after exiting PCLA for `myBuildSet` would fix `Ev`
into the only existing constraint at that moment, i.e., to `Base1` and then when got back to the nested lambda analysis would report
a `TYPE_MISMATCH` in the `it.add(Derived2())` because the variable has been already fixed to less permissive type.

But that looks counter-intuitive, and moreover, the example above was green in Builder Inference implementation in K1.

Thus, for PCLA, we decided to analyze return statements with FULL completion mode even if the lambda return type contains not-yet-inferred
type variables.

Though, this solutuion is not totally universal: it might make some more "red" than it is if we postponed the nested lambdas in return 
statements.

For example:
```kotlin
fun <E> myBuildSet2(transformer: (MutableSet<E>) -> E): MutableSet<E> = TODO()

fun main(b: Boolean) {
    myBuildSet2 { builder ->
        if (b) {
            return@myBuildSet2 { arg ->
                arg.length // Unresolved reference: `.length` because we don't have proper constraints here
            }
        }
        builder.add({ x: String -> })
    }
}
```

There, we start the analysis of the first lambda analysis before we'd come to the second one where the type hint is given, thus having
an unresolved reference.

But it's been decided to go with this solution, thus allowing inferring to a green code within the first example with `myBuildSet`.
**NB:** This second example didn't work in K1.

For details, see `expectedTypeForReturnArguments` definition at
`org.jetbrains.kotlin.fir.resolve.inference.PostponedArgumentsAnalyzer.analyzeLambda`.

## On demand variable fixation

Unlike non-PCLA context, here there might be legit situations when someone needs to look into member scope of some type variable.

For example, see the following case:
```kotlin
buildList {
    // MutableList<Ev> <: Tv
    // T can't be fixed, because it's deeply related to Ev
    this.let { it -> // it: Tv
        it.add("") // requesting member scope of Tv
    }
}
```

There, we've got `it` used as a receiver in a call `it.add("")`, thus the first question we need to answer to is which member scope it has.

And the answer is that before starting the selector (`add("")`), we simply try to fix necessary type variable with the following steps:
- Temporary override what *proper type* means
  - Not fixed type variables might be used as a type argument for a proper type (e.g., `MutableList<Ev>` is proper).
  - But they cannot be used as top-level proper type (e.g, we can't fix `Tv` to `Ev`).
  - See `ConstraintSystemCompletionContext.withTypeVariablesThatAreCountedAsProperTypes` 
    and `TypeSystemInferenceExtensionContext.isProperTypeForFixation` extension.
- If there is some proper type for that TV, run regular result type computation `ResultType`.
- Add constraint `Tv := ResultType` and use `ResultType` as a member scope
- **NB:** we don't actually *fix* the variable via `NewConstraintSystemImpl.fixVariable`, because there's an assumption that resulting
type should not contain other type variables that might be not satisfied during on-demand fixation. But `fixVariable` would anyway be called
during final completion when all other type variables types would be properly substituted in the given equality constraint.

As follows, from the rules above, there are some scenarios where the variable can't be fixed. In those cases, we implicitly forbid 
using them as receivers:
- By assuming that we've got their member scope empty
- Reporting a diagnostic at `org.jetbrains.kotlin.fir.resolve.calls.TypeVariablesInExplicitReceivers` resolution stage
  (currently `BUILDER_INFERENCE_STUB_RECEIVER` being used).

```kotlin
fun <T> myLet(t: T, b: (T) -> Unit) {} 

buildList {
    get(0).toString() // BUILDER_INFERENCE_STUB_RECEIVER is reported with PCLA
    
    myLet(get(0)) { it ->
        get(0).toString() // BUILDER_INFERENCE_STUB_RECEIVER is reported with PCLA
    }
}
```

See the implementation details at `FirPCLAInferenceSession.fixCurrentResultIfTypeVariableAndReturnBinding`.

## CST computation

When computing the result type for TV referencing other variables from outer CS there might be a situation when we need to compute 
`CommonSuperType(Xv, ...)`:

```kotlin
fun <S> selectL(x: S, y: S, l: (S) -> Unit) {}

fun foo(x: MutableList<String>) {
    buildList {
        // Adding the constraints
        // MutableList<String> <: Sv
        // MutableList<Ev <: Sv
        // But to analyze lambda we fix Sv to CST(MutableList<String>, MutableList<Ev>) = MutableList<CST(String, Ev)>
        // The question is what is CST(String, Ev)
        selectL(x, this) { x ->
            x.add(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
            x.add("") 
        }
    } // inferred to List<String>
}
```

**References:** `org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver.prepareLowerConstraints`

In general, such a problem existed and be resolved before PCLA:
- On the fixation stage, type variable should have at least one proper constraint, but other might be not-proper, but still being used
- For that case, we do a controversial thing, namely
  - Substituting all TVs with special stub types
  - Compute CST with those substituted types
    - For those stub types, there’s effectively a rule that for any type `Tv` and stub `XStub`: `CST(Tv, XStub) = Tv`
- Intuitively, such an approach seems correct because it just chooses some result and incorporates it with other TVs, at least not hiding any contradictions
  - In the example above, we would get `Sv := MutableList<String>` and during incorporation we would assert the subtyping `Collection<S> <: Collection<String>`, thus having constraint `S <: String` making the whole CS sound

But unlike a non-PCLA case, our TV might have a single constraint containing a reference to some other (outer) TV, so we need to make 
some tweaks:
- Because we've overridden what "proper" means here, but `prepareLowerConstraints` uses it in a sense of the usage some TV inside the type,
for PCLA context, we unconditionally perform stub substitution (a bit of dirty place)
- And after that we might need to substitute "local" stubs back to TVs 
  (impossible in non-PCLA, because in that case, at least one constraint is "proper" in regular sense, i.e., doesn't contain any TVs)

## Nested PCLA

```kotlin
buildList { 
    add("")
    
    addAll(buildSet l2@{ 
        add(1)
    })
}
```

For nested PCLA lambdas, everything works pretty straightforward:
- We create own `FirPCLAInferenceSession` for it
- Run analysis of the nested lambda with that session just the same way as for an outer
  - But the main candidate for `l2` automatically uses the shared CS for `l1` (thus, it's used as an outer CS for `l2`)
- But we don't run completion writing results at this stage yet
- Just applying the system for `l2` to the candidate of nested candidate owning the lambda ( `addAll` in the example), and after that it
  automatically propagates to the shared CS during `processPartiallyResolvedCall`.

Thus, in the end, we would get the CS for the root call containing all variables from the whole PCLA-tree.

Completion results writing for nested lambdas happen recursively when it starts for the root call:
- In the example we start with `buildList`.
- Go to `FirCallCompletionResultsWriterTransformer.runPCLARelatedTasksForCandidate`.
- Find the postponed call `addAll`.
- Recursively run `runPCLARelatedTasksForCandidate` for it at some point.
- And while iterating the postponed call for the latter will write results for `add(1)` call.

## Nested Delegation inference

```kotlin
buildList {
    val x by lazy {
        get(0)
    }
    
    add("")
    add(x)
}
```

Inference for delegated properties inside PCLA works in the following way:
- Delegate expression is being resolved as a regular postponed nested call under the same PCLA session.
- All the operator calls are being resolved under specially created delegate `FirDelegatedPropertyInferenceSession` in `PARTIAL` mode.
- *After* that we run completion for delegate expression via `PCLA_POSTPONED_CALL` mode.
- Then, we integrate delegation related CS into PCLA shared session and all the delegation related calls as regular postponed ones
  of the PCLA session.

For more details, read [delegated_property_inference.md](delegated_property_inference.md), 
`FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot` and `FirPCLAInferenceSession.integrateChildSession`.
