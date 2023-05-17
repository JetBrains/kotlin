## FIR/Builder inference
See also: [Kotlin Spec: Builder-style type inference](https://kotlinlang.org/spec/type-inference.html#builder-style-type-inference)

### Glossary
#### CS = Constraint system
An instance of `org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl`
#### Call-tree
A tree of calls, in which constraint systems are joined and solved(completed) together
#### Postponed type variable
Type-variable, that was used to build a stub type on, and fixation of which is postponed until the end of lambda analysis.
Selection of type variables to postpone happens during [Initiation](#initiation) phase
#### Stub type
Type, that is equal to anything from the perspective of subtyping.
Carries information about its base postponed type variable

Such types are used during [analysis of lambda body](#lambda-body-analysis) inplace of postponed type variables to resolve calls,
after end of analysis occurrences of such types are replaced with corresponding type inference result for its base type variable

Ex:
```kotlin
buildList { /* this: MutableList<Stub(T)> -> */
    val self/*: MutableList<Stub(T)> */ = this
    self.add("") // String <: Stub(T) from argument
}
```
#### Proper constraint
A constraint that doesn't reference any type variables
### Builder inference algorithm
The algorithm consists of the following phases, all of which happen during constraint system completion
- [Initiation](#initiation) - deciding if lambda is suitable for builder inference
- [Lambda analysis preparation](#lambda-analysis-preparation) - stub type and builder inference session creating
- [Lambda body analysis](#lambda-body-analysis) - collecting calls inside lambda body, which references stub-types among input types
- [Lambda analysis finalization](#lambda-analysis-finalization) - integrating collected information
    - [Result write](#result-write) - writing of builder inference result to FIR tree of lambda (and implicit receiver stack update)
    - [Result backpropagation](#result-backpropagation) - propagation of builder inference result to CS of call-tree

### Detailed description
#### Initiation
Before, we try to fix all type variables that have enough type info to be fixed and analyze all lambda arguments, 
whose input types depend on such type variables. 

If there are still not analyzed lambda arguments left, we try to perform builder inference

Entrypoint to builder inference is the function `org.jetbrains.kotlin.fir.resolve.inference.ConstraintSystemCompleter.tryToCompleteWithBuilderInference`

*It happens only during full completion*

Then, all lambda arguments, that weren't analyzed and satisfy the following criteria are considered:

- lambda argument has a non-empty list of input types
- any of the input types contain a non-fixed type variable in arguments (If the input type is a type-variable itself, it is **NOT** considered)

Such type-variables are marked as postponed and analysis of the lambda argument is performed

#### Lambda analysis preparation
First, `org.jetbrains.kotlin.fir.resolve.inference.PostponedArgumentsAnalyzer.analyzeLambda`
- creates stub types for all postponed type variables
- substitutes type-variable-type -> stub-type in a receiver, context receiver, parameters, and return types

Then, before the actual transforming of the lambda body, an instance of `org.jetbrains.kotlin.fir.resolve.inference.FirBuilderInferenceSession`
is created and set as the current inference session

#### Lambda body analysis
During body analysis of a lambda, for each call (inside lambda body) that is completed in the FULL mode, we ask the inference session if it can
be completed

See `org.jetbrains.kotlin.fir.resolve.inference.FirBuilderInferenceSession.shouldRunCompletion`

It is needed in cases, where the result of builder inference can help with further system completion

Example:
```kotlin
fun <M> materialize(): M = null!!
fun foo() = buildList {
    addAll(materialize()) // (1) To be able to infer type for materialize, we need to know builder inference result
    add("str") // (2) This call already has complete type info, so we allow it to complete
}
```

In situation 1, we mark the call as an incomplete, see `org.jetbrains.kotlin.fir.resolve.inference.FirBuilderInferenceSession.addPartiallyResolvedCall`
In situation 2, we mark the call as a completed, see `org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession.addCompletedCall`

Note: Calls that were analyzed in PARTIAL mode will not be considered, as it always analyzed during FULL completion of the outer call.
Such calls will not be added to incomplete/completed call lists

The criteria to mark a call as an incomplete is **ALL** following:
1. There's no contradiction in the constraint system of the call
2. The candidate (call) is suitable for builder inference based on **ANY** of the following conditions:
    - The dispatch receiver's type contains a stub type
    - The extension receiver's type contains a stub type and the corresponding symbol has a builder inference annotation in the session
3. The candidate doesn't have any not analyzed postponed atoms
4. Any type variable in call CS doesn't have a proper constraint, and it's not a postponed variable

If call is marked incomplete its constraint system wouldn't be solved as usual in `org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter.completeCall`, completion result writing will not be performed.
It is postponed until [lambda analysis finalization](#lambda-analysis-finalization)

#### Lambda analysis finalization
See `org.jetbrains.kotlin.fir.resolve.inference.PostponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem`

Once the lambda body was analyzed return arguments are added into the call-tree

> ##### Note: Incomplete calls in return arguments
> We don't add last expression of lambda to the call-tree as a return argument if its functional-type has Unit return-type, to
> avoid situations when last expression contains incomplete call, see [Incomplete call in return arguments](#incomplete-call-in-return-arguments)

Then, we perform inference of postponed type variables

See `org.jetbrains.kotlin.fir.resolve.inference.FirBuilderInferenceSession.inferPostponedVariables`

After such steps, the constraint system of the call-tree contains all output-type constraints

While systems for the completed and incomplete calls inside the lambda body contain all internal type constraints in the form of
constraints with stub types

See `org.jetbrains.kotlin.fir.resolve.inference.FirBuilderInferenceSession.buildCommonSystem`

To find a solution for postponed type variables we need to integrate all aforementioned constraints.
We do so, by constructing a new constraint system in the following way:
- Register all not-fixed type variables from CS of call-tree
- Re-apply all initial constraints from CS of call-tree
    - Substitute Stub(PostponedTV) with TypeVariableType(PostponedTV)
    - Optimization: Only constraints, where at least one of the types contains TypeVariableType from integration system are applied
    - Constraints, that originate from builder inference results of other lambda arguments of the call-tree are skipped (See `org.jetbrains.kotlin.resolve.calls.inference.model.BuilderInferencePosition`)
- Re-apply all initial constraints of all completed calls that were collected during lambda body analysis
    - Using the same algorithm as above
- Re-apply all initial constraints of all incomplete calls
    - Using the same algorithm as above
    - Also, register all fixed type variables and bring its fixation result as an equality constraint

Note: Such a system shouldn't contain stub types from current builder inference session in constraints anymore

If the system is empty, we bail out and skip to the [result write](#result-write)

After such a constraint system is constructed, we call the constraint system completer to solve the system

Note: As incomplete calls couldn't contain postponed arguments in the call-tree (as defined in [lambda body analysis](#lambda-body-analysis)), we don't analyze any postponed arguments here

#### Result write
See `org.jetbrains.kotlin.fir.resolve.inference.FirBuilderInferenceSession.updateCalls`

##### Resulting substitutor:
Solution(TV) - result type, that was inferred for a type variable TV in integration CS

Substitution will be the following:
- Stub(PostponedTV) -> Solution(PostponedTV) ||
- TypeVariableType(TV) -> Solution(TV) || 
- Stub(*) -> ERROR || 
- TypeVariableType(*) -> ERROR

Note: Effectively, it means that if we have an inference result for a postponed type variable, we replace stub/type variable types inside with
an error types

Unclear: If the constraint system wasn't empty we apply the resulting substitutor to fixed type variables from CS of the call-tree

We apply the resulting substitutor to all type references inside of lambda, recursively

After that, we apply the same substitution to all the implicit receivers, which originated from lambdas. It is needed to have full type information
during the analysis of lambda arguments inside of return arguments in the current lambda
(since those arguments may be analyzed after builder inference completion)

Then, we call the completion results writer to store completion results for incomplete calls with the result substitutor

#### Result backpropagation
See `org.jetbrains.kotlin.fir.resolve.inference.PostponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem`

Once the result for postponed variables is inferred and stored, we need to propagate results to the CS of call-tree
since its completion isn't finished yet

To do so, we inject subtype constraint with result type from postponed variable inference

After that, we mark postponed type variables as no longer postponed 

Lambda analysis is now finished, and we return to the [Initiation](#initiation) to process other lambda arguments of the call-tree

### Potential problems
#### Unclear naming
In fact, builder inference doesn't only apply to "builders". It's a general algorithm that applies to lambda arguments of particular shape

Ex: 
```kotlin
suspendCoroutine/* <String> inferred by 'builder inference' */ {
    it.resume("")
}
// ...
fun <T> consumeIterator(input: Iterator<T>.() -> Unit) {}
fun takeString(s: String) {}
//...
consumeIterator/* <String> inferred by 'builder inference' */ { takeString(next()) }
```
#### Incomplete criteria to perform builder-inference
Criteria to perform builder inference doesn't consider lambdas, which input types contain only type-variable-types
#### Incomplete criteria to mark calls as incomplete during lambda analysis
- Criteria to mark calls as incomplete depends on builder inference annotation, however it shouldn't be used anymore
- In case of present postponed arguments somewhere in the call-tree it will change behavior, failing to infer types
- Ad-hoc check for the presence of proper constraints
#### Lost diagnostics
The constraint system that is used to infer results for postponed type variables isn't checked for inconsistencies
#### Unclear substitution of fixed type variables
During [result writing](#result-write), we update fixed type variables of the main call-tree, reasons for that aren't clear
#### Potentially exponential complexity
- During [lambda analysis finalization](#lambda-analysis-finalization) we copy initial constraints from call-tree to integration system, it
can lead to exponential complexity (no concrete example here)
- During [result writing](#result-write) we traverse body of lambda argument recursively, in case if there are multiple nested lambdas that are
subjects to builder inference, it will be exponential
#### Separate constraint system creation and result backpropagation
During [lambda analysis finalization](#lambda-analysis-finalization) we create and solve separate CS. 

However, we need to communicate results to the CS of the call-tree, during [result backpropagation](#result-backpropagation) we add subtype constraints between 
type variable from the call-tree and solution from integration CS, loosing information about actual constraints. 

It causes unsound solutions in the call-tree CS
#### Resulting substitution is unclear
Its unclear why we mix stub type substitutor with type-variable-type substitutor, and why we need to manually handle substitution to error
types
#### Incomplete call in return arguments
If incomplete call is present among return arguments during [Lambda analysis finalization](#lambda-analysis-finalization) we add it to the 
main call-tree, but then complete it as part of [result writing](#result-write).

It leads to violation of contract in `org.jetbrains.kotlin.fir.resolve.inference.ConstraintSystemCompleter.getOrderedAllTypeVariables` as type variables for completed call couldn't be found anymore

To avoid such problem, we have [workaround for lambdas with Unit return-type](#Note-Incomplete-calls-in-return-arguments), but problem still 
occurs:
- When we have [lambdas with non-Unit return-type](https://youtrack.jetbrains.com/issue/KT-58741)
- When the [incomplete call is present in a return argument that isn't last expression](https://youtrack.jetbrains.com/issue/KT-58742)