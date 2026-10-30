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
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<E3>` _from ExpectedType for some call_

### Call 5

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<E5>` _from ExpectedType for some call_

### Call 6

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<kotlin/collections/List<E6>>` _from ExpectedType for some call_

### Call 7

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<out E7>` _from ExpectedType for some call_

### Call 8

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<out kotlin/collections/List<E8>>` _from ExpectedType for some call_

### Call 9

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Consumer<E4>` _from ExpectedType for some call_

### Call 10

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(A#())
    x# = setOfB#
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = mySetOf1#(A#())↩    x# = setOfB#↩    x#.size#↩}↩_

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

### Call 11

```
mySetOf1#(R|/A.A|())
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf1` --- `fun <E1> mySetOf1(x: E1): Set<E1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E1)` for `FirNamedFunctionSymbol /mySetOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(E1)` _from Argument R|/A.A|()_

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

### Call 12

```
setOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/setOfB` --- `setOfB: Set<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/Set<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 13

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<I>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(E1) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `A <: TypeVariable(E1)` with `TypeVariable(E1) <: TypeVariable(_CST_0)`
    1. `A <: TypeVariable(_CST_0)`
4. `B <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
5. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 14

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

### Call 15

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = setOfB#
    x# = mySetOf1#(A#())
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = setOfB#↩    x# = mySetOf1#(A#())↩    x#.size#↩}↩_

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

### Call 16

```
setOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/setOfB` --- `setOfB: Set<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/Set<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 17

```
mySetOf1#(R|/A.A|())
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf1` --- `fun <E1> mySetOf1(x: E1): Set<E1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E1)` for `FirNamedFunctionSymbol /mySetOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(E1)` _from Argument R|/A.A|()_

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

### Call 18

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<I>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `B <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. `TypeVariable(E1) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
4. Combine `A <: TypeVariable(E1)` with `TypeVariable(E1) <: TypeVariable(_CST_0)`
    1. `A <: TypeVariable(_CST_0)`
5. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 19

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

### Call 20

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = setOfA#
    x# = mySetOf2#(B#())
    x# = setOfB#
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = setOfA#↩    x# = mySetOf2#(B#())↩    x# = setOfB#↩    x#.size#↩}↩_

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

### Call 21

```
setOfA#
```

#### Candidate 1: `FirValueParameterSymbol <local>/setOfA` --- `setOfA: Set<A>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/Set<A> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<A> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 22

```
mySetOf2#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf2` --- `fun <E2> mySetOf2(x: E2): Set<E2>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E2)` for `FirNamedFunctionSymbol /mySetOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E2)` _from Argument R|/B.B|()_

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

### Call 23

```
setOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/setOfB` --- `setOfB: Set<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/Set<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 24

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<I>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `A <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. `TypeVariable(E2) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
4. Combine `B <: TypeVariable(E2)` with `TypeVariable(E2) <: TypeVariable(_CST_0)`
    1. `B <: TypeVariable(_CST_0)`
5. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 25

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

### Call 26

```
mySetOf1#(IntegerLiteral(2))
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf1` --- `fun <E1> mySetOf1(x: E1): Set<E1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E1)` for `FirNamedFunctionSymbol /mySetOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 2 <: TypeVariable(E1)` _from Argument IntegerLiteral(2)_

##### Call Completion:

1. Choose `TypeVariable(E1)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(E1) == kotlin/Int` _from Fix variable E1_

### Call 27

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf2#(String(1))
    x# = mySetOfInt#
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = mySetOf2#(String(1))↩    x# = mySetOfInt#↩    x#.size#↩}↩_

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

### Call 28

```
mySetOf2#(String(1))
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf2` --- `fun <E2> mySetOf2(x: E2): Set<E2>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E2)` for `FirNamedFunctionSymbol /mySetOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E2)` _from Argument String(1)_

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

### Call 29

```
mySetOfInt#
```

#### Candidate 1: `FirLocalPropertySymbol <local>/mySetOfInt` --- `local val mySetOfInt: Set<Int>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/Set<kotlin/Int> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<kotlin/Int> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 30

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<it(Comparable<*> & Serializable)>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(E2) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `kotlin/String <: TypeVariable(E2)` with `TypeVariable(E2) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
4. `kotlin/Int <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
5. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 31

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

### Call 32

```
R|<local>/box|.R|SubstitutionOverride</Box.x: R|kotlin/collections/Set<it(kotlin/Comparable<*> & java/io/Serializable)>|>|.R|SubstitutionOverride<kotlin/collections/Set.iterator: R|kotlin/collections/Iterator<it(kotlin/Comparable<*> & java/io/Serializable)>|>|().next#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/Iterator.next` --- `fun next(): it(Comparable<*> & Serializable)`
##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `it(kotlin/Comparable<*> & java/io/Serializable) <: kotlin/Comparable<*>` _from ExpectedType for some call_

### Call 33

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: NullableBox<B>` _from ExpectedType for some call_

### Call 34

```
setOf#(IntegerLiteral(42))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(element: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 42 <: TypeVariable(T)` _from Argument IntegerLiteral(42)_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 42 <: TypeVariable(T)` _from Argument IntegerLiteral(42)_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 34

```
setOf#(IntegerLiteral(42))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(element: T): Set<T>`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(T)> <: kotlin/collections/Set<kotlin/Int>` _from ExpectedType for some call_
    1. `TypeVariable(T) <: kotlin/Int`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
2. `TypeVariable(T) == kotlin/Int` _from Fix variable T_

### Call 35

```
buildNullableBox#(<L> = buildNullableBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    b# = setInt#
    b# = setOf#(String(42))
    b#?.{ $subj$.size# }
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildNullableBox` --- `fun <B> buildNullableBox(block: NullableBox<B>.() -> Unit): NullableBox<B>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(B)` for `FirNamedFunctionSymbol /buildNullableBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `NullableBox<TypeVariable(B)>.() -> kotlin/Unit <: NullableBox<TypeVariable(B)>.() -> kotlin/Unit` _from Argument <L> = buildNullableBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    b# = setInt#↩    b# = setOf#(String(42))↩    b#?.{ $subj$.size# }↩}↩_

##### Call Completion:

1. Choose `TypeVariable(B)` with `Readiness(
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
2. Choose `TypeVariable(B)` with `Readiness(
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

### Call 36

```
setInt#
```

#### Candidate 1: `FirLocalPropertySymbol <local>/setInt` --- `local val setInt: Set<Int>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/Set<kotlin/Int> <: TypeVariable(B)?` _from ExpectedType for some call_
    1. `kotlin/collections/Set<kotlin/Int> <: TypeVariable(B)`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<kotlin/Int> <: TypeVariable(B)?` _from ExpectedType for some call_
    1. `kotlin/collections/Set<kotlin/Int> <: TypeVariable(B)`

### Call 37

```
setOf#(String(42))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(element: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(42)_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(42)_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 37

```
setOf#(String(42))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(element: T): Set<T>`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(B)?` _from ExpectedType for some call_
    1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(B)`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(B)?` _from ExpectedType for some call_
    1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(B)`

### Call 38

```
b#
```

#### Candidate 1: `FirRegularPropertySymbol /NullableBox.b` --- `var b: Set<it(Comparable<*> & Serializable)>?`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `kotlin/Int <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. `TypeVariable(T) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
4. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
5. `TypeVariable(B) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable B_

### Call 39

```
$subj$.size#
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

### Call 35

```
buildNullableBox#(<L> = buildNullableBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    b# = setInt#
    b# = setOf#(String(42))
    b#?.{ $subj$.size# }
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildNullableBox` --- `fun <B> buildNullableBox(block: NullableBox<B>.() -> Unit): NullableBox<B>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(_CST_0)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
3. `TypeVariable(T) == kotlin/String` _from Fix variable T_
4. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
5. `kotlin/collections/Set<kotlin/String> <: TypeVariable(B)` _from Fix variable T_
6. Choose `TypeVariable(_CST_0)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
    1. `TypeVariable(B)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
7. `TypeVariable(_CST_0) == it(kotlin/Comparable<*> & java/io/Serializable)` _from Fix variable _CST_0_
8. `TypeVariable(B) == kotlin/collections/Set<it(kotlin/Comparable<*> & java/io/Serializable)>` _from Fix variable _CST_0_
9. Choose `TypeVariable(B)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 40

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf#(B#())
    x# = mutableSetOfB#
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOf#(B#())↩    x# = mutableSetOfB#↩    x#.add#(B#())↩}↩_

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

### Call 41

```
myMutableSetOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf` --- `fun <E3> myMutableSetOf(x: E3): MutableSet<E3>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E3)` for `FirNamedFunctionSymbol /myMutableSetOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E3)` _from Argument R|/B.B|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E3)` with `Readiness(
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

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 42

```
mutableSetOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfB` --- `mutableSetOfB: MutableSet<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 43

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<B>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_
2. Combine `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<B>`
    1. `TypeVariable(E3) <: B`
    2. `B <: TypeVariable(E3)`
3. Combine `B <: TypeVariable(E3)` with `TypeVariable(E3) <: B`
    1. `TypeVariable(E3) == B`

### Call 44

```
this@R|special/anonymous|.R?C|/Box.x|.add#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: B): Boolean`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 40

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf#(B#())
    x# = mutableSetOfB#
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E3)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(E3) == B` _from Fix variable E3_
4. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from Fix variable E3_
5. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_

### Call 45

```
R|/buildBox|<R|kotlin/collections/MutableSet<B>|>(<L> = buildBox@fun R|Box<kotlin/collections/MutableSet<B>>|.<anonymous>(): R|kotlin/Unit| <inline=NoInline>  {
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|/myMutableSetOf|<R|B|>(R|/B.B|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|<local>/mutableSetOfB|
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>|.R|SubstitutionOverride<kotlin/collections/MutableSet.add: R|kotlin/Boolean|>|(R|/B.B|())
}
).R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>|.add#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: B): Boolean`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 46

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf#(A#())
    x# = mutableSetOfB#
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOf#(A#())↩    x# = mutableSetOfB#↩    x#.add#(B#())↩}↩_

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

### Call 47

```
myMutableSetOf#(R|/A.A|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf` --- `fun <E3> myMutableSetOf(x: E3): MutableSet<E3>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E3)` for `FirNamedFunctionSymbol /myMutableSetOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(E3)` _from Argument R|/A.A|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E3)` with `Readiness(
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

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 48

```
mutableSetOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfB` --- `mutableSetOfB: MutableSet<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 49

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<B>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_
2. Combine `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<B>`
    1. `TypeVariable(E3) <: B`
    2. `B <: TypeVariable(E3)`
3. __NewConstraintError: `A <: B`__
4. Combine `TypeVariable(E3) <: B` with `B <: TypeVariable(E3)`
    1. `TypeVariable(E3) == B`
5. __NewConstraintError: `A <: B`__

### Call 46

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf#(A#())
    x# = mutableSetOfB#
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. Choose `TypeVariable(E3)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(E3) == B` _from Fix variable E3_
3. __NewConstraintError: `A <: B`__
4. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from Fix variable E3_
5. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_

### Call 50

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf#(B#())
    x# = myMutableSetOf2#(B#())
    x# = mutableSetOfB#
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOf#(B#())↩    x# = myMutableSetOf2#(B#())↩    x# = mutableSetOfB#↩    x#.add#(B#())↩}↩_

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

### Call 51

```
myMutableSetOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf` --- `fun <E3> myMutableSetOf(x: E3): MutableSet<E3>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E3)` for `FirNamedFunctionSymbol /myMutableSetOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E3)` _from Argument R|/B.B|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E3)` with `Readiness(
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

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 52

```
myMutableSetOf2#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf2` --- `fun <E5> myMutableSetOf2(x: E5): MutableSet<E5>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E5)` for `FirNamedFunctionSymbol /myMutableSetOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E5)` _from Argument R|/B.B|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E5)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E5)` with `Readiness(
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

1. `kotlin/collections/MutableSet<TypeVariable(E5)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 53

```
mutableSetOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfB` --- `mutableSetOfB: MutableSet<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 54

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<B>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_
2. Combine `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<B>`
    1. `TypeVariable(E3) <: B`
    2. `B <: TypeVariable(E3)`
3. Combine `kotlin/collections/MutableSet<TypeVariable(E5)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<B>`
    1. `TypeVariable(E5) <: B`
    2. `B <: TypeVariable(E5)`
4. Combine `B <: TypeVariable(E3)` with `TypeVariable(E3) <: B`
    1. `TypeVariable(E3) == B`
5. Combine `B <: TypeVariable(E5)` with `TypeVariable(E5) <: B`
    1. `TypeVariable(E5) == B`

### Call 55

```
this@R|special/anonymous|.R?C|/Box.x|.add#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: B): Boolean`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 50

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf#(B#())
    x# = myMutableSetOf2#(B#())
    x# = mutableSetOfB#
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E3)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(E5)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(E3) == B` _from Fix variable E3_
4. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from Fix variable E3_
5. Choose `TypeVariable(E5)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
6. `TypeVariable(E5) == B` _from Fix variable E5_
7. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
8. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_

### Call 56

```
R|/buildBox|<R|kotlin/collections/MutableSet<B>|>(<L> = buildBox@fun R|Box<kotlin/collections/MutableSet<B>>|.<anonymous>(): R|kotlin/Unit| <inline=NoInline>  {
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|/myMutableSetOf|<R|B|>(R|/B.B|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|/myMutableSetOf2|<R|B|>(R|/B.B|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|<local>/mutableSetOfB|
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>|.R|SubstitutionOverride<kotlin/collections/MutableSet.add: R|kotlin/Boolean|>|(R|/B.B|())
}
).R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>|.add#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: B): Boolean`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 57

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mutableSetOfB#
    x# = myMutableSetOf#(B#())
    x# = myMutableSetOf2#(B#())
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = mutableSetOfB#↩    x# = myMutableSetOf#(B#())↩    x# = myMutableSetOf2#(B#())↩    x#.add#(B#())↩}↩_

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

### Call 58

```
mutableSetOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfB` --- `mutableSetOfB: MutableSet<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 59

```
myMutableSetOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf` --- `fun <E3> myMutableSetOf(x: E3): MutableSet<E3>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E3)` for `FirNamedFunctionSymbol /myMutableSetOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E3)` _from Argument R|/B.B|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E3)` with `Readiness(
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

1. `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 60

```
myMutableSetOf2#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf2` --- `fun <E5> myMutableSetOf2(x: E5): MutableSet<E5>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E5)` for `FirNamedFunctionSymbol /myMutableSetOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E5)` _from Argument R|/B.B|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E5)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E5)` with `Readiness(
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

1. `kotlin/collections/MutableSet<TypeVariable(E5)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 61

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<B>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_
2. Combine `kotlin/collections/MutableSet<TypeVariable(E3)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<B>`
    1. `TypeVariable(E3) <: B`
    2. `B <: TypeVariable(E3)`
3. Combine `kotlin/collections/MutableSet<TypeVariable(E5)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<B>`
    1. `TypeVariable(E5) <: B`
    2. `B <: TypeVariable(E5)`
4. Combine `B <: TypeVariable(E3)` with `TypeVariable(E3) <: B`
    1. `TypeVariable(E3) == B`
5. Combine `B <: TypeVariable(E5)` with `TypeVariable(E5) <: B`
    1. `TypeVariable(E5) == B`

### Call 62

```
this@R|special/anonymous|.R?C|/Box.x|.add#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: B): Boolean`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 57

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mutableSetOfB#
    x# = myMutableSetOf#(B#())
    x# = myMutableSetOf2#(B#())
    x#.add#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E3)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(E5)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(E3) == B` _from Fix variable E3_
4. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from Fix variable E3_
5. Choose `TypeVariable(E5)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
6. `TypeVariable(E5) == B` _from Fix variable E5_
7. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
8. `TypeVariable(Z) == kotlin/collections/MutableSet<B>` _from Fix variable Z_

### Call 63

```
R|/buildBox|<R|kotlin/collections/MutableSet<B>|>(<L> = buildBox@fun R|Box<kotlin/collections/MutableSet<B>>|.<anonymous>(): R|kotlin/Unit| <inline=NoInline>  {
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|<local>/mutableSetOfB|
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|/myMutableSetOf|<R|B|>(R|/B.B|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>| = R|/myMutableSetOf2|<R|B|>(R|/B.B|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>|.R|SubstitutionOverride<kotlin/collections/MutableSet.add: R|kotlin/Boolean|>|(R|/B.B|())
}
).R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<B>|>|.add#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: B): Boolean`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 64

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOfLists#(B#())
    x# = mutableSetOfListsOfB#
    x#.add#(listOf#(B#()))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOfLists#(B#())↩    x# = mutableSetOfListsOfB#↩    x#.add#(listOf#(B#()))↩}↩_

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

### Call 65

```
myMutableSetOfLists#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOfLists` --- `fun <E6> myMutableSetOfLists(x: E6): MutableSet<List<E6>>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E6)` for `FirNamedFunctionSymbol /myMutableSetOfLists`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(E6)` _from Argument R|/B.B|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E6)>> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E6)` with `Readiness(
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

1. `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E6)>> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 66

```
mutableSetOfListsOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfListsOfB` --- `mutableSetOfListsOfB: MutableSet<List<B>>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<kotlin/collections/List<B>> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<kotlin/collections/List<B>> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 67

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<List<B>>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<B>>` _from Fix variable Z_
2. Combine `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E6)>> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<B>>`
    1. `TypeVariable(E6) <: B`
    2. `B <: TypeVariable(E6)`
3. Combine `B <: TypeVariable(E6)` with `TypeVariable(E6) <: B`
    1. `TypeVariable(E6) == B`

### Call 68

```
listOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(T)` _from Argument R|/B.B|()_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(T)` _from Argument R|/B.B|()_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 68

```
listOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
2. `TypeVariable(T) == B` _from Fix variable T_

### Call 69

```
this@R|special/anonymous|.R?C|/Box.x|.add#(R|kotlin/collections/listOf|<R|B|>(R|/B.B|()))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: List<B>): Boolean`
##### Resolution Stages > CheckArguments:

1. `kotlin/collections/List<B> <: kotlin/collections/List<B>` _from Argument R|kotlin/collections/listOf|<R|B|>(R|/B.B|())_

### Call 64

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOfLists#(B#())
    x# = mutableSetOfListsOfB#
    x#.add#(listOf#(B#()))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E6)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(E6) == B` _from Fix variable E6_
4. `kotlin/collections/MutableSet<kotlin/collections/List<B>> <: TypeVariable(Z)` _from Fix variable E6_
5. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<B>>` _from Fix variable Z_

### Call 70

```
listOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(T)` _from Argument R|/B.B|()_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `B <: TypeVariable(T)` _from Argument R|/B.B|()_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 70

```
listOf#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
2. `TypeVariable(T) == B` _from Fix variable T_

### Call 71

```
R|/buildBox|<R|kotlin/collections/MutableSet<kotlin/collections/List<B>>|>(<L> = buildBox@fun R|Box<kotlin/collections/MutableSet<kotlin/collections/List<B>>>|.<anonymous>(): R|kotlin/Unit| <inline=NoInline>  {
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<kotlin/collections/List<B>>|>| = R|/myMutableSetOfLists|<R|B|>(R|/B.B|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<kotlin/collections/List<B>>|>| = R|<local>/mutableSetOfListsOfB|
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<kotlin/collections/List<B>>|>|.R|SubstitutionOverride<kotlin/collections/MutableSet.add: R|kotlin/Boolean|>|(R|kotlin/collections/listOf|<R|B|>(R|/B.B|()))
}
).R|SubstitutionOverride</Box.x: R|kotlin/collections/MutableSet<kotlin/collections/List<B>>|>|.add#(R|kotlin/collections/listOf|<R|B|>(R|/B.B|()))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: List<B>): Boolean`
##### Resolution Stages > CheckArguments:

1. `kotlin/collections/List<B> <: kotlin/collections/List<B>` _from Argument R|kotlin/collections/listOf|<R|B|>(R|/B.B|())_

### Call 72

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOfOut#(A#())
    x# = mutableSetOfB#
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOfOut#(A#())↩    x# = mutableSetOfB#↩    x#.size#↩}↩_

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

### Call 73

```
myMutableSetOfOut#(R|/A.A|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOfOut` --- `fun <E7> myMutableSetOfOut(x: E7): MutableSet<out E7>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E7)` for `FirNamedFunctionSymbol /myMutableSetOfOut`s parameter 0

##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(E7)` _from Argument R|/A.A|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<out TypeVariable(E7)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E7)` with `Readiness(
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

1. `kotlin/collections/MutableSet<out TypeVariable(E7)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 74

```
mutableSetOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfB` --- `mutableSetOfB: MutableSet<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 75

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<out I>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(E7) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `A <: TypeVariable(E7)` with `TypeVariable(E7) <: TypeVariable(_CST_0)`
    1. `A <: TypeVariable(_CST_0)`
4. `B <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
5. `TypeVariable(Z) == kotlin/collections/MutableSet<out TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 76

```
this@R|special/anonymous|.R?C|/Box.x|.size#
```

#### Candidate 1: `FirRegularPropertySymbol kotlin/collections/MutableSet.size` --- `val size: Int`
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

### Call 77

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOfOutLists#(A#())
    x# = mutableSetOfListsOfB#
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOfOutLists#(A#())↩    x# = mutableSetOfListsOfB#↩    x#.size#↩}↩_

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

### Call 78

```
myMutableSetOfOutLists#(R|/A.A|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOfOutLists` --- `fun <E8> myMutableSetOfOutLists(x: E8): MutableSet<out List<E8>>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E8)` for `FirNamedFunctionSymbol /myMutableSetOfOutLists`s parameter 0

##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(E8)` _from Argument R|/A.A|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<out kotlin/collections/List<TypeVariable(E8)>> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E8)` with `Readiness(
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

1. `kotlin/collections/MutableSet<out kotlin/collections/List<TypeVariable(E8)>> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 79

```
mutableSetOfListsOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/mutableSetOfListsOfB` --- `mutableSetOfListsOfB: MutableSet<List<B>>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `kotlin/collections/MutableSet<kotlin/collections/List<B>> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/MutableSet<kotlin/collections/List<B>> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 80

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<out List<I>>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(E8) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `A <: TypeVariable(E8)` with `TypeVariable(E8) <: TypeVariable(_CST_0)`
    1. `A <: TypeVariable(_CST_0)`
4. `B <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
5. `TypeVariable(Z) == kotlin/collections/MutableSet<out kotlin/collections/List<TypeVariable(_CST_0)>>` _from Fix variable Z_

### Call 81

```
this@R|special/anonymous|.R?C|/Box.x|.size#
```

#### Candidate 1: `FirRegularPropertySymbol kotlin/collections/MutableSet.size` --- `val size: Int`
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

### Call 82

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myConsumerOf#(A#())
    x# = consumerOfB#
    x#.consume#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myConsumerOf#(A#())↩    x# = consumerOfB#↩    x#.consume#(B#())↩}↩_

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

### Call 83

```
myConsumerOf#(R|/A.A|())
```

#### Candidate 1: `FirNamedFunctionSymbol /myConsumerOf` --- `fun <E4> myConsumerOf(x: E4): Consumer<E4>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E4)` for `FirNamedFunctionSymbol /myConsumerOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(E4)` _from Argument R|/A.A|()_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `Consumer<TypeVariable(E4)> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(E4)` with `Readiness(
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

1. `Consumer<TypeVariable(E4)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 84

```
consumerOfB#
```

#### Candidate 1: `FirValueParameterSymbol <local>/consumerOfB` --- `consumerOfB: Consumer<B>`
##### Resolution Stages > TypeVariablesInExplicitReceivers:

1. `Consumer<B> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `Consumer<B> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 85

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Consumer<B>`
##### Call Completion:

1. `TypeVariable(Z) == Consumer<B>` _from Fix variable Z_
2. Combine `Consumer<TypeVariable(E4)> <: TypeVariable(Z)` with `TypeVariable(Z) == Consumer<B>`
    1. `B <: TypeVariable(E4)`

### Call 86

```
this@R|special/anonymous|.R?C|/Box.x|.consume#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /Consumer.consume` --- `fun consume(t: B): Unit`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_

### Call 82

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myConsumerOf#(A#())
    x# = consumerOfB#
    x#.consume#(B#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E4)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(E4) == I` _from Fix variable E4_
4. `Consumer<I> <: TypeVariable(Z)` _from Fix variable E4_
5. Choose `TypeVariable(Z)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(Z) == Consumer<B>` _from Fix variable Z_

### Call 87

```
R|/buildBox|<R|Consumer<B>|>(<L> = buildBox@fun R|Box<Consumer<B>>|.<anonymous>(): R|kotlin/Unit| <inline=NoInline>  {
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|Consumer<B>|>| = R|/myConsumerOf|<R|I|>(R|/A.A|())
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|Consumer<B>|>| = R|<local>/consumerOfB|
    this@R|special/anonymous|.R|SubstitutionOverride</Box.x: R|Consumer<B>|>|.R|SubstitutionOverride</Consumer.consume: R|kotlin/Unit|>|(R|/B.B|())
}
).R|SubstitutionOverride</Box.x: R|Consumer<B>|>|.consume#(R|/B.B|())
```

#### Candidate 1: `FirNamedFunctionSymbol /Consumer.consume` --- `fun consume(t: B): Unit`
##### Resolution Stages > CheckArguments:

1. `B <: B` _from Argument R|/B.B|()_