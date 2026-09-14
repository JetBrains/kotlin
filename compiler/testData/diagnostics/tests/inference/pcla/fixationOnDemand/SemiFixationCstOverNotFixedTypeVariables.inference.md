## `Source session for module <main>`

### Call 1

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Box<Z>` _from ExpectedType for some call_

### Call 2

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/Set<E1>` _from ExpectedType for some call_

### Call 3

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/Set<E2>` _from ExpectedType for some call_

### Call 4

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(String(1))
    x# = mySetOf2#(String(2))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = mySetOf1#(String(1))↩    x# = mySetOf2#(String(2))↩    x#.size#↩}↩_

##### Call Completion:

1. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 5

```
mySetOf1#(String(1))
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf1` --- `fun <E1> mySetOf1(x: E1): Set<E1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E1)` for `FirNamedFunctionSymbol /mySetOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E1)` _from Argument String(1)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(E1)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E1)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<TypeVariable(E1)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 6

```
mySetOf2#(String(2))
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf2` --- `fun <E2> mySetOf2(x: E2): Set<E2>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E2)` for `FirNamedFunctionSymbol /mySetOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E2)` _from Argument String(2)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(E2)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E2)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<TypeVariable(E2)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 7

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<String>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(E1) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `kotlin/String <: TypeVariable(E1)` with `TypeVariable(E1) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
4. `TypeVariable(E2) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
5. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 8

```
this@R|special/anonymous|.R?C|/Box.x|.size#
```

#### Candidate 1: `FirRegularPropertySymbol kotlin/collections/Set.size` --- `val size: Int`
##### Call Completion:

1. Choose `TypeVariable(_CST_0)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`