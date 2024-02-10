# Inference

Currently, this document contains some basic terms that are common for different specific inference types.
Lately, it might be extended to include some basic description of how inference works.

## Glossary
### TV = type variable
To avoid confusion with type parameter types, we mostly use `Tv` for referencing type variable based on a type parameter named `T`
### CS = Constraint system
- Mostly, it’s just a collection of TVs and the constraints for them in a form of `Xi <: SomeType` or `SomeType <: Xi` or `Xi = SomeType`
- Represented as an instance of `org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl`

### Related TVs

See `TypeVariableDependencyInformationProvider` for details.

#### Shallow relation
Two variables `Xv` and `Yv` are shallowly related if there is some chain of a constraints
`Xv ~ a_1 ~ .. ~ a_n ~ Yv` where (`~` is either `<:` or `>:`)

#### Deep relation
Two variables `Xv` and `Yv` are deeply related if there is some chain of a constraints
`a_1 ~ .. ~ a_n` where (`~` is either `<:` or `>:`) **and** `a_1` contains `Xv` while `a_n` contains `Yv`.

### Call-tree
A tree of calls, in which constraint systems are joined and solved(completed) together
### Proper constraint
A constraint that doesn't reference any type variables
### Input types of a lambda
- Receiver type
- Value parameter types

**Completion** is a process that for a given call, its CS and postponed atoms (lambdas and callable references) tries to infer some TV and analyze some lambdas

**Completion mode**

Completion mode (`ConstraintSystemCompletionMode`) defines how actively/forcefully the given call is being completed
* `PARTIAL` — used for calls that are other calls arguments
    * It prevents from fixing TV that are related somehow to return types because their actual result depends on the chosen outer call
    * Otherwise, it tries to infer as much as possible, but doesn’t lead to reporting not-inferred-type-parameter or running PCLA lambdas
    * On one hand, we might’ve not run that kind of completion at all, and just accumulate nested argument calls into an outer as is, 
    * but inferring something allows disambiguating outer candidates
    * ```
      val x: Int = 1
      fun main() {
          x.plus(run { x })
      }
      ```
* `FULL` — used for top statement-level calls and receivers
    * It tries to fix/analyze everything we have
    * And start PCLA analysis if necessary
    * Reports errors if for some TV there’s no enough information to infer them
* `UNTIL_FIRST_LAMBDA` — used for OverloadByLambdaReturnType
    * Similar to FULL, but stops after first lambda analyzed
* `PCLA_POSTPONED_CALL` — see [pcla.md](pcla.md).

## Inference session

A set of callbacks related to inference that being called during function body transformations.
See `FirInferenceSession`.
