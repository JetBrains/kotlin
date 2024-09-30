# Type inference for calls

## Intro

Type inference is a quite broad topic, and here we focus only on the type inference for calls, i.e., on the algorithm trying to guess which
type arguments would fit properly for a given call if they're not specified explicitly.

For example, in the case of a call `listOf("")`, it would be quite nice to understand that the user expects it to work the same way as
`listOf<String>("")`.

All other cases of type inference, e.g., property type inference like `val x = ""` is beside the scope of the document.

**TODO**: For the sake of simplicity, we don't cover callable references here, shall be done later.

### Given input

Let's assume that we've got some independent call on a statement level inside the body block:

```kotlin
fun main() {
    listOf("")
}
```

- And some candidate with the appropriate value arguments number has already been chosen.
- But the candidate function has its type parameters, for which type arguments aren't specified.

In general, the situation might be a bit more complicated.

The independent call may have dependent call-arguments (see [Call tree](#call-tree)):

```kotlin
fun <K> materialize(): K = TODO()

fun main() {
    listOf("", materialize())
}
```

Or it might have lambdas and/or expected type

```kotlin
fun main() {
    val x: List<Any> = listOf("", run { 1 })
}
```

### Expected output

- For all calls in the call-tree, for each type parameter, there should be a type argument chosen.
- For all lambdas, all receiver/parameter/return types should be computed.

**TODO:** Cover how we infer the function kind of lambda (regular/suspend/compose).

```kotlin
fun main() {
    val x: List<Any> = listOf<Any>("", run<Int>(fun(): Int = 1))
}
```

Or, if there are some type contradictions, the error should be reported.

### Naive algorithm description

The idea of this part is to give some basic idea of how the algorithm works:

- At first step, we create a constraint system ([CS](#cs--constraint-system)) with type variables associated with all type parameters in the
  call-tree.
- Gather type variables constraints from the call tree (e.g. `Tv <: String` in `listOf("")`).
- Analyze lambdas' bodies after choosing types of their parameters (once it's possible).
- For each type variable, from the gathered constraints, choose the most reasonable resulting type.

## Detailed description

In general, the call resolution algorithm works in a very natural recursive manner:

- During body resolution, when we recursively traverse the AST and meet a call, before doing something, we would recursively resolve
  its receiver and arguments.
- But the arguments are being resolved in a special way, so the algorithm is aware that it's ok if some information (from an expected type)
  is missing, so it leaves them partially resolved until the resolution of the top-level call begins.
- So, when we start the actual Resolution & Inference process, the argument-calls have their own chosen candidates and Constraint Systems.

### Context dependency

Calls resolution modes might be separated into two kinds:

- Independent (potentially with an expected type): regular top-level statements, receivers, assignments RHS, etc.
- Dependent: arguments of other calls or return expressions of lambdas

```kotlin

fun main() {
    listOf("") // Independent
    val x: List<Any> = listOf("") // Independent with expected type

    listOf( // Independent
        listOf("") // Dependent
    )

    listOf( // Independent
        run { // Dependent
            listOf("") // Independent
            listOf("") // Dependent
        }
    )

    listOf( // Independent
        ""
    ).get(0) // Independent
}
```

**NB:** The calls that are used as receivers are being completed as `Independent` and it's crucial because our inference algorithm
doesn't allow type-constraints to flow back from the call to its receiver (like `mutableMapOf().put("", "")` is red code).

See `org.jetbrains.kotlin.fir.resolve.ResolutionMode`.

One of the most important use cases for the resolution mode is defining whether a call should be **fully** [completed](#completion-mode).

### Constraint System Creation

At the first step of any candidate call resolution, we create a constraint system ([CS](#cs--constraint-system)), which has:

- A specific dedicated type variable ([TV](#tv--type-variable-fresh-type-variable)) for each type parameter in the call-tree.
- A set of constraints for each type variable in a form of `Tv <: SomeType` (UPPER), `SomeType <: Tv` (LOWER)
  or `Tv := SomeType` (EQUALITY).
- Basic constraints, obtained from the relevant type parameters upper bounds, e.g. `Tv <: Closeable` for
  `fun <T : Closeable> T.use(...)`.

**NB**: in the case of complex call-trees (`parentCall(argumentCall())`) the parent calls accumulate CS of all the arguments and add
their own type variables.

**NB**: CS is being created for each candidate, even for ones that later might be declared as inapplicable.
Even the candidates with no type parameters should have their CS (the empty one).

See `org.jetbrains.kotlin.fir.resolve.calls.stages.CreateFreshTypeVariableSubstitutorStage`.

The substitutor from type parameter types to fresh type variables is stored at `Candidate::substitutor`.

### Candidate Resolution Phase

For each candidate we perform a bunch of resolution stages (see `org.jetbrains.kotlin.fir.resolve.calls.candidate.CallKind`), each of them
either works with its CS by introducing new [initial constraints](#initial-constraints),
or in some other ways checks if the candidate is applicable.

The most illustrative example of the resolution stage is `CheckArguments` which effectively adds argument-related initial constraints
in the form of `ValueArgumentType <: ValueParameterType`.

**NB:** During candidate resolution it's not allowed to go into lambda analysis because the signature of the anonymous function might
depend on the candidate and there may be more than one candidate.
And resolution of the same lambda body more than once would make the whole call resolution algorithm exponential.

After that, if the Constraint System contains contradiction, we discard the candidate and go to the next one
(or report an error for the last one).

Otherwise, if we've got a single perfect candidate, we choose it and go to the *Completion Stage*.

### Initial Constraints

*Initial constraint* is an arbitrary type restriction obtained from code semantics.

The most basic example of such a constraint is that a value argument type must be a subtype of a parameter type.
Or it might be an expected type in case like `val x: List<String> = listOf()` (initial constraint would be `List<Tv> <: List<String>`).

Without losing generality, we might say that all the initial constraints have the form of `ArbitraryType1 <: ArbitraryType2`.

And their main purpose is deriving *type variable constraints* (`Tv <: SomeType`, `SomeType <: Tv`, or `Tv := SomeType`).

### Deriving Constraints

The process of deriving constraints works through the regular subtyping algorithm with a special callback catching attempt to check
if some type is a subtype of a type variable or vice versa.

```kotlin

fun <T> foo(x: Collection<T>) {}

fun bar(x: MutableList<String>) {
    foo(x)
}
```

For example, in the case above:

- We add an initial constraint `MutableList<String> <: Collection<Tv>`
- When checking, the subtyping algorithm looks for a matching type with a matching type constructor for subtyping, i.e.,`Collection<String>`
- Thus, the subtyping check is reduced to `Collection<String> <: Collection<Tv>`.
- For such cases, we check each type argument and as we've got the single covariant parameter
- Thus, we deduce that `String <: Tv` and got a new lower constraint on `Tv`.

For more details, see `org.jetbrains.kotlin.types.AbstractTypeChecker.isSubtypeOf` and
`org.jetbrains.kotlin.resolve.calls.inference.components.TypeCheckerStateForConstraintSystem.addSubtypeConstraint` as an example of the
overridden callback for catching a type variable comparison case.

One of the potential results of the subtyping algorithm might be a failure
if some intermediate constraint `ArbitraryType1 <: ArbitraryType2` cannot be satisfied.

For example, `MutableMap<String, Int> <: Map<Tv, String>` would be derived to `Int <: String` constraint
which cannot be satisfied.

In this situation, we say that CS has a *contradiction*.

### Constraint Incorporation

Once there's a new *type variable constraint*, it might make sense to derive new ones from it using constraints derived in previous steps
and relying on subtyping transitivity.

If a new *incorporated variable constraint* has been added, we repeat the process recursively.

There are two kinds of incorporation.

#### Direct With The Same Variable

If the new constraint has a form of `Tv <: SomeType`, for each existing lower (opposite) constraint `OtherType <: Tv`, we may add
a new *initial constraint* `OtherType <: SomeType`.

```kotlin
fun <I> id(x: I): I = x

fun <T> listOf(): List<T> = TODO()

fun main() {
    val x: List<String> = id(listOf())
}
```

In the above example we would have:

- Initial constraint `List<Tv> <: Iv` (which immediately adds a lower constraint on `Iv`)
- `Iv <: List<String>` from expected type (which immediately adds an upper constraint on `Iv`)
- Thus, from the transitive closure of `List<Tv> <: Iv` and `Iv <: List<String>` we might deduce
  a new initial constraint `List<Tv> <: List<String>`
- From which we finally may derive an upper constraint `Tv <: String`

#### Inside Other Constraints

If the new constraint has a form of `Tv <: SomeType`, for each other type variable `Fv` which contains `Tv` inside its constraints,
for each such constraint `Fv <: OtherType<..Tv..>`, we make a tricky substitution of `Tv` inside `OtherType<..>` with some approximation of
`SomeType` which would give a new **correct** initial constraint.

- In a simple case of `Fv <: Tv`, we would add `Fv <: SomeType`.
- In more complex `Fv <: Inv<Tv>`, it would be `Fv <: Inv<out SomeType>`.

For more details, see `org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintIncorporator.insideOtherConstraint`

### Completion Stage

We have the following information at this stage:

- a chosen candidate for each call of the call-tree
- a single Constraint System containing type variables from the entire call-tree together with their constraints
  - additionally, no contradictions have been found in the Constraint System yet
- in case of an `Independent` call, possibly, an expected type (which, if present, also served as a basis for an initial constraint earlier)

It's worth noting that no lambda argument from the call-tree has been analyzed yet.

**Completion** is a process that, given the aforementioned information, tries:
- to infer a proper type for each type variable of the call-tree
- to analyze all lambda arguments from the call-tree

### Completion Modes

Completion modes (`ConstraintSystemCompletionMode`) define how actively/forcefully a given call-tree should be completed.

#### `FULL` completion mode

Used for call-trees in top-statement and receiver positions.

- tries to fix every type variable from the Constraint System
- reports errors if there’s not enough information to fix a type variable
- processes lambdas via [PCLA](#partially-constrained-lambda-analysis) if necessary and possible

#### `PARTIAL` completion mode

Used for call-trees in value-argument positions.

- doesn't try to fix type variables from the Constraint System that are related to the calls' return types
  - (fixation of such type variables depends on the results of overload resolution for the containing call)
- doesn't report errors if there’s not enough information to fix a type variable
  - (such a type variable could be fixed later during `FULL` completion of the top-level containing call-tree)
- doesn't process lambdas via [PCLA](#partially-constrained-lambda-analysis) (but does process lambdas via regular lambda analysis if possible)
  - (see [the "PCLA entry point" section of pcla.md](pcla.md#pcla-entry-point))

One could argue that the `PARTIAL` mode could or/and should have been removed
in favor of performing a single `FULL` completion of the entire top-level containing call-tree;
however, `PARTIAL` completion of value arguments can be necessary to perform overload resolution for containing calls:

```
fun foo(arg: Int) {}
fun foo(arg: Double) {}

fun <R> run(block: () -> R): R = block()

fun example(int: Int) {
    // a candidate for this call cannot be chosen
    // without fixing Rv to Int first
    foo(
        // Rv cannot be fixed to Int
        // without analyzing this call's lambda argument first
        run(
            // PARTIAL completion mode makes it possible to analyze this lambda
            // before a candidate for the `foo` call has to be chosen
            { int }
        )
    )
}
```

#### `PCLA_POSTPONED_CALL` completion mode

A modification of `FULL` completion mode.
Used for [PCLA](#partially-constrained-lambda-analysis).
See [the "PCLA_POSTPONED_CALL completion mode" section of pcla.md](pcla.md#pcla_postponed_call-completion-mode).

#### `UNTIL_FIRST_LAMBDA` completion mode

A modification of `FULL` completion mode that stops after the first lambda analyzed.
Used for overload resolution by a lambda's return type.

### Completion Framework

Roughly, the framework algorithm might be represented in the form of the following pseudocode, some parts of which will be described
in the following sections

```kotlin
while (true) {
    if (analyzeSomeLambdaWithProperInputTypes()) continue

    if (findAndFixReadyTypeVariable()) continue

    if (completionMode == FULL) if (analyzeSomeLambdaWithPCLA()) continue

    if (completionMode == FULL) reportNotInferredTypeVariable()
}
```

After the loop is completed in `FULL` mode, the completion results are being applied to the FIR call representations via
`FirCallCompletionResultsWriterTransformer`.

For more details see `FirCallCompleter` and `org.jetbrains.kotlin.fir.resolve.inference.ConstraintSystemCompleter.runCompletion` for the
actual representation of the Main While Loop.

**NB:** From the definition of the loop, it shouldn't be very hard to show that it converges in polynomial time
(if all the operations are polynomial).

### Basic Lambda analysis

If for some lambda all its [input types](#input-types-of-a-lambda) are proper, it's possible to start its regular analysis.

All its statements beside return ones (including the last expression) might be resolved as usual/independently.

If the return type of the lambda is [proper](#proper-typeconstraint), all return expressions (the subjects of return statements) should
be analyzed and completed independently (with return type as an ExpectedType).

Otherwise, they are being analyzed in `Dependent` mode and for each of them:

- It becomes a part of a root call-tree.
  For example, if we've got `return@lambda listOf()`, the type argument for the call should be inferred via the root completion phase
- We add another initial constraint `ReturnedExpressionType <: LambdaReturnType`.

### Inference Heuristics

With given CS data input and the given framework, there might be more than one "consistent" result.

For example, for the top-level [independent](#context-dependency) call `listOf("")` might-be inferred as `listOf<Any>("")` in a sense
that this result doesn't lead to any type contradiction, but that might surprise a user who
has a property `val myList = listOf("")` which type would be implicitly inferred to `List<Any>`.

The [Completion Framework](#completion-framework) defines a number of degrees of freedom which might be tuned to give different results.

And the following sections describe them and the state chosen for them in the attempt to have the most reasonable results.

**NB:** Those degrees of freedom do not just define the resulting type arguments but potentially affect if the inference process converges
successfully.
For example, too early type variable fixation (e.g., before analysis of a lambda with a return type containing the variable) might lead
to contradictions which wouldn't be there if we analyzed the lambda beforehand.

### Variable Selection

During the completion some of the type variables become *fixed*, i.e., the final type for
them is chosen, and all occurrences of the variable inside other constraints are replaced with that type.
Also, this type is used as a type argument when completion ends.

Thus, such a fixed type variable stops being an active part of the Constraint System.

#### Variables that cannot be fixed

Some of the variables that are not yet fixed might be forbidden to fix at this point.

The most obvious reason is the lack of any [proper constraint](#proper-constraint), thus there's not just enough information yet to choose
the type.

Another reason might be relevant for [dependent](#context-dependency) calls: a type variable is forbidden to fix if it is related to
the return type of the call.
The explanation for this rule is that this particular call is an argument of another call for which we don't know yet what candidate would
be chosen.
Thus don't know a proper expected type that might in fact contradict the chosen result type.

```kotlin
fun foo(x: MutableList<Any>, y: Int) {}
fun foo(x: MutableList<String>, y: String) {}

fun main() {
    // Resolved to the first overload
    foo(
        // String <: Tv
        // Potentially, we might've fixed Tv to String when resolving `mutableListOf("")`.
        // But that would lead to `MutableList<String> <: MutableList<Int>` contradiction
        mutableListOf(""),
        1
    )
}
```

#### Variables order

All other variables (that might be fixed) are being sorted according to a set of heuristics, and then the first one is chosen to be fixed.

**NB:** if two variables are equal from heuristics' point-of-view, the one that has a shorter path in a call-tree is chosen
(see `org.jetbrains.kotlin.fir.resolve.inference.ConstraintSystemCompleter.getOrderedAllTypeVariables` for details).

When it comes to heuristics, we consider the following factors:

- if a variable is contained in an input type of lambda (see
  `PostponedArgumentInputTypesResolver.fixNextReadyVariableForParameterTypeIfNeeded`)
- If a variable has dependency (constraints) related to another not-fixed variable
- If a variable is used in any output types (return types of lambdas)
- Or even if it's made upon a reified type parameter

But for the exact ordering, it's better to look at the freshest version of `VariableFixationFinder.getTypeVariableReadiness` as it made
in a quite descriptive form.

### Result Type For Fixation

Once the variable is chosen, we should decide to which result type it should be fixed.

**Input:**

- Non-contradictory state of Constraint System
- The list of constraints for the type variable (at least one of them must be proper)
- Constraints of other type variables that potentially might be related

At first, we look if there are some `EQUAL` constraints and choose first of them which doesn't contain integer literal types
(**NB:** anyway all of them should not contradict each other, so in some broad sense all such constraints are equal).

Otherwise, we choose a representative **subtype** and **supertype** if applicable.

For **subtype**, we select all the lower constraints and compute a *common supertype of them*.
A common supertype is expected to be the lowest upper bound on the type lattice, i.e., it should be a supertype
of all the lower constraints, but it should be as "precise" as possible (see `NewCommonSuperTypeCalculator`).

In case of non-proper lower constraints, we replace type variables in them with special *stub types* which behave like they're equal to
anything.

For example, here:

```kotlin
fun <F> select(f1: F, f2: F): F = TODO()

fun main() {
    select(
        // MutableList<String> <: Fv
        mutableListOf<String>(),
        // List<Tv> <: Fv
        emptyList()
    )
}
```

We don't yet have simple constraints for fixing `Tv`, so going to fix `Fv` first:

- It has two lower constraints: `List<Tv>` (`List<StubType>` after stub-type substitution) and `MutableList<String>`.
- Their common supertype is computed to `List<String>` (with the help of specially defined equality on stub-types).
- Thus, we fix `Fv := List<String>` and then `Tv := String`.

We don't cover that part extensively here, but it's guaranteed that the resulting type would not contain nor a type variable or a stub type.

For more details, look into `ResultTypeResolver.findSubType`

For **supertypes**, it's just an intersection of all *proper* types.

After that, *Captured Type Approximation* is applied to both of the candidate types
(if the approximated version does not lead to contradictions).

And in most cases if both candidate types exist, we choose the subtype (if it's not `Nothing?`), or return the single candidate type
(see `ResultTypeResolver.resultType` for details).

### Partially Constrained Lambda Analysis

In most cases, a failure to fix a type variable during `FULL` completion leads to a compilation error.
By the point a `FULL` completion is reached,
most of the useful type constraints that are usually available have already been added to the Constraint System;
this is because relevant type information cannot be found "outside" calls that are completed in the `FULL` mode.

However, if any lambda arguments are not processed by the end of a `FULL` completion,
helpful type information might still be present inside them;
the only reason a lambda argument would not be processed by that point is if its input types contained not-fixed type variables in question.
Therefore, as a last resort in such situations, we can try analyzing such lambda arguments in a limited capacity.

The most common example of when such analysis is necessary is a call to a "builder-like" function:

```kotlin
fun <E> buildList(builderAction: MutableList<E>.() -> Unit): List<E> { /* ... */ }

fun main() {
    buildList { // Ev := ????
        add("") // Ev <: String
    } // Ev := String
}
```

In the example above, there is not enough information to fix `Ev` of the `buildList` call by the end of a `FULL` completion
nor are there any additional sources of type information available aside from the `add("")` call inside the body of the lambda argument.

The problem with performing such analysis naively is that we usually don't allow using type variables inside independent statements.
In situations like the `buildList` example, we have to do that, which necessitates a separate mode of lambda analysis.
We call this mode the _partially constrained lambda analysis_ (_PCLA_ for short).

See [pcla.md](pcla.md) for more information.

## Glossary

### Type variable (TV)

Fresh type variables (or just type variables for short) are special types (and type constructors) that are created:
* for each type parameter of a called declaration
* per call of said declaration

The second clause is relevant in situations when the same type parameter is encountered twice in the same call-tree,
e.g., `listOf(listOf(""))`:
a total of two type variables (one for each of the two calls) would be created in this example even though there is only one type parameter.

To avoid mixing up type variables with the corresponding type parameters and with each other, we often use the following conventions:
* a type variable `Tv` is based on a type parameter `T`
* type variables `Tv1` and `Tv2` are two different type variables based on the same type parameter `T`

There are also synthetic type variables that represent unknown parameter types and/or return types of lambdas.

### Type constraint

A type relation between a type variable and another type in one of the following forms:
- `Tv <: AnotherType`
- `AnotherType <: Tv`
- `Tv == AnotherType`

### Proper type

A type that doesn't contain any (not-yet-fixed) type variables.

### Proper constraint

A constraint that is imposed on a type variable by a proper type.

### Constraint system (CS)

A collection of type variables and constraints imposed on them.

**Source code representation:** `org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl`

### Related type variables

**Source code reference:** `TypeVariableDependencyInformationProvider`

#### Shallow relation

Two type variables `Xv` and `Yv` are **shallowly related** if:
- there exists a type relation chain `Xv ~ T_1 ~ .. ~ T_n ~ Yv` (where `~` is either `<:` or `>:`)

#### Deep relation

Two type variables `Xv` and `Yv` are **deeply related** if:
- there exists a type relation chain `T_1 ~ .. ~ T_n` (where `~` is either `<:` or `>:`)
- `T_1` contains `Xv`
- `T_n` contains `Yv`

### Call-tree

A tree of calls for which constraint systems of each call are joined and solved (completed) together.

### Postponed atoms

- Lambda argument of a call
- Callable reference argument of a call

### Input types of a postponed atom

- Type of the postponed atom's receiver
- Types of the postponed atom's value parameters

### Inference session

A set of callbacks related to inference that are called during function body transformations.

**Source code representation:** `FirInferenceSession`
