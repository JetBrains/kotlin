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

1. `kotlin/Nothing <: Box<Z>` _from ExpectedType for some call_

### Call 3

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/Set<E1>` _from ExpectedType for some call_

### Call 4

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/Set<E2>` _from ExpectedType for some call_

### Call 5

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<E3>` _from ExpectedType for some call_

### Call 6

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<E4>` _from ExpectedType for some call_

### Call 7

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<kotlin/collections/List<E7>>` _from ExpectedType for some call_

### Call 8

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: kotlin/collections/MutableSet<kotlin/collections/List<E8>>` _from ExpectedType for some call_

### Call 9

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Consumer<E5>` _from ExpectedType for some call_

### Call 10

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Consumer<E6>` _from ExpectedType for some call_

### Call 11

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

### Call 12

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

### Call 13

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

### Call 14

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<*>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/Set<*>` _from Fix variable Z_

### Call 11

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(String(1))
    x# = mySetOf2#(String(2))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E1)` with `Readiness(
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
    2. `TypeVariable(E2)` is `Readiness(
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
3. `TypeVariable(E1) == kotlin/String` _from Fix variable E1_
4. `kotlin/collections/Set<kotlin/String> <: TypeVariable(Z)` _from Fix variable E1_
5. Choose `TypeVariable(E2)` with `Readiness(
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
6. `TypeVariable(E2) == kotlin/String` _from Fix variable E2_
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
8. `TypeVariable(Z) == kotlin/collections/Set<*>` _from Fix variable Z_

### Call 15

```
buildBoundedBox#(<L> = buildBoundedBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(String(1))
    x# = mySetOf2#(String(2))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBoundedBox` --- `fun <Z : Collection<Any>> buildBoundedBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBoundedBox`s parameter 0
2. `TypeVariable(Z) <: kotlin/collections/Collection<kotlin/Any>` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBoundedBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = mySetOf1#(String(1))↩    x# = mySetOf2#(String(2))↩    x#.size#↩}↩_

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
mySetOf1#(String(1))
```

#### Candidate 1: `FirNamedFunctionSymbol /mySetOf1` --- `fun <E1> mySetOf1(x: E1): Set<E1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E1)` for `FirNamedFunctionSymbol /mySetOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E1)` _from Argument String(1)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(E1)> <: TypeVariable(Z)` _from ExpectedType for some call_
2. Combine `kotlin/collections/Set<TypeVariable(E1)> <: TypeVariable(Z)` with `TypeVariable(Z) <: kotlin/collections/Collection<kotlin/Any>`
    1. `TypeVariable(E1) <: kotlin/Any`

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

### Call 17

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
2. Combine `kotlin/collections/Set<TypeVariable(E2)> <: TypeVariable(Z)` with `TypeVariable(Z) <: kotlin/collections/Collection<kotlin/Any>`
    1. `TypeVariable(E2) <: kotlin/Any`

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

### Call 18

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<*>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/Set<*>` _from Fix variable Z_
2. __NewConstraintError: `kotlin/collections/Set<*> <: kotlin/collections/Collection<kotlin/Any>`__

### Call 15

```
buildBoundedBox#(<L> = buildBoundedBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(String(1))
    x# = mySetOf2#(String(2))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBoundedBox` --- `fun <Z : Collection<Any>> buildBoundedBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

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
    2. `TypeVariable(E2)` is `Readiness(
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
2. `TypeVariable(E1) == kotlin/String` _from Fix variable E1_
3. `kotlin/collections/Set<kotlin/String> <: TypeVariable(Z)` _from Fix variable E1_
4. Choose `TypeVariable(E2)` with `Readiness(
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
5. `TypeVariable(E2) == kotlin/String` _from Fix variable E2_
6. Choose `TypeVariable(Z)` with `Readiness(
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
7. `TypeVariable(Z) == kotlin/collections/Set<*>` _from Fix variable Z_
8. __NewConstraintError: `kotlin/collections/Set<*> <: kotlin/collections/Collection<kotlin/Any>`__

### Call 19

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(String(1))
    x# = java#.util#.Collections#.emptySet#()
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = mySetOf1#(String(1))↩    x# = java#.util#.Collections#.emptySet#()↩    x#.size#↩}↩_

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

### Call 20

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

### Call 21

```
Q|java/util/Collections|.emptySet#()
```

#### Candidate 1: `FirNamedFunctionSymbol java/util/Collections.emptySet` --- `static fun <T : Any!> emptySet(): ft<MutableSet<T!>, Set<T!>?>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol java/util/Collections.emptySet`s parameter 0

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `ft<kotlin/collections/MutableSet<TypeVariable(T)!>, kotlin/collections/Set<TypeVariable(T)!>?> <: TypeVariable(Z)` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
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

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `ft<kotlin/collections/MutableSet<TypeVariable(T)!>, kotlin/collections/Set<TypeVariable(T)!>?> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 22

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: ft<Set<*>, Set<*>?>`
##### Call Completion:

1. `TypeVariable(Z) == ft<kotlin/collections/Set<*>, kotlin/collections/Set<*>?>` _from Fix variable Z_

### Call 19

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = mySetOf1#(String(1))
    x# = java#.util#.Collections#.emptySet#()
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E1)` with `Readiness(
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
    2. `TypeVariable(T)` is `Readiness(
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
3. `TypeVariable(E1) == kotlin/String` _from Fix variable E1_
4. `kotlin/collections/Set<kotlin/String> <: TypeVariable(Z)` _from Fix variable E1_
5. Choose `TypeVariable(Z)` with `Readiness(
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
    1. `TypeVariable(T)` is `Readiness(
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
6. `TypeVariable(Z) == ft<kotlin/collections/Set<*>, kotlin/collections/Set<*>?>` _from Fix variable Z_
7. Choose `TypeVariable(T)` with `Readiness(
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
8. Choose `TypeVariable(T)` with `Readiness(
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
9. __NotEnoughInformationForTypeParameter__
10. `TypeVariable(T) == ERROR CLASS: Cannot infer argument for type parameter T` _from Fix variable T_

### Call 23

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf1#(String(1))
    x# = myMutableSetOf2#(String(2))
    x#.add#(String(3))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOf1#(String(1))↩    x# = myMutableSetOf2#(String(2))↩    x#.add#(String(3))↩}↩_

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

### Call 24

```
myMutableSetOf1#(String(1))
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf1` --- `fun <E3> myMutableSetOf1(x: E3): MutableSet<E3>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E3)` for `FirNamedFunctionSymbol /myMutableSetOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E3)` _from Argument String(1)_

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

### Call 25

```
myMutableSetOf2#(String(2))
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOf2` --- `fun <E4> myMutableSetOf2(x: E4): MutableSet<E4>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E4)` for `FirNamedFunctionSymbol /myMutableSetOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E4)` _from Argument String(2)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<TypeVariable(E4)> <: TypeVariable(Z)` _from ExpectedType for some call_

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

1. `kotlin/collections/MutableSet<TypeVariable(E4)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 26

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<*>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<*>` _from Fix variable Z_

### Call 23

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOf1#(String(1))
    x# = myMutableSetOf2#(String(2))
    x#.add#(String(3))
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
    2. `TypeVariable(E4)` is `Readiness(
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
3. `TypeVariable(E3) == kotlin/String` _from Fix variable E3_
4. `kotlin/collections/MutableSet<kotlin/String> <: TypeVariable(Z)` _from Fix variable E3_
5. Choose `TypeVariable(E4)` with `Readiness(
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
6. `TypeVariable(E4) == kotlin/String` _from Fix variable E4_
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
8. `TypeVariable(Z) == kotlin/collections/MutableSet<*>` _from Fix variable Z_

### Call 27

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOfLists1#(String(1))
    x# = myMutableSetOfLists2#(String(2))
    x#.add#(listOf#(String(3)))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myMutableSetOfLists1#(String(1))↩    x# = myMutableSetOfLists2#(String(2))↩    x#.add#(listOf#(String(3)))↩}↩_

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
myMutableSetOfLists1#(String(1))
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOfLists1` --- `fun <E7> myMutableSetOfLists1(x: E7): MutableSet<List<E7>>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E7)` for `FirNamedFunctionSymbol /myMutableSetOfLists1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E7)` _from Argument String(1)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E7)>> <: TypeVariable(Z)` _from ExpectedType for some call_

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

1. `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E7)>> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 29

```
myMutableSetOfLists2#(String(2))
```

#### Candidate 1: `FirNamedFunctionSymbol /myMutableSetOfLists2` --- `fun <E8> myMutableSetOfLists2(x: E8): MutableSet<List<E8>>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E8)` for `FirNamedFunctionSymbol /myMutableSetOfLists2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E8)` _from Argument String(2)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E8)>> <: TypeVariable(Z)` _from ExpectedType for some call_

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

1. `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E8)>> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 30

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: MutableSet<List<*>>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<*>>` _from Fix variable Z_
2. Combine `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E7)>> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<*>>`
    1. `kotlin/Any? <: TypeVariable(E7)`
3. Combine `kotlin/collections/MutableSet<kotlin/collections/List<TypeVariable(E8)>> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<*>>`
    1. `kotlin/Any? <: TypeVariable(E8)`

### Call 31

```
listOf#(String(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(3)_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(3)_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 31

```
listOf#(String(3))
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
2. `TypeVariable(T) == kotlin/String` _from Fix variable T_

### Call 32

```
this@R|special/anonymous|.R?C|/Box.x|.add#(R|kotlin/collections/listOf|<R|kotlin/String|>(String(3)))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/MutableSet.add` --- `@IgnorableReturnValue() fun add(element: List<*>): Boolean`
##### Resolution Stages > CheckArguments:

1. `kotlin/collections/List<kotlin/String> <: kotlin/collections/List<*>` _from Argument R|kotlin/collections/listOf|<R|kotlin/String|>(String(3))_

### Call 27

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myMutableSetOfLists1#(String(1))
    x# = myMutableSetOfLists2#(String(2))
    x#.add#(listOf#(String(3)))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E7)` with `Readiness(
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
    2. `TypeVariable(E8)` is `Readiness(
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
3. `TypeVariable(E7) == kotlin/Any?` _from Fix variable E7_
4. `kotlin/collections/MutableSet<kotlin/collections/List<kotlin/Any?>> <: TypeVariable(Z)` _from Fix variable E7_
5. Choose `TypeVariable(E8)` with `Readiness(
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
6. `TypeVariable(E8) == kotlin/Any?` _from Fix variable E8_
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
8. `TypeVariable(Z) == kotlin/collections/MutableSet<kotlin/collections/List<*>>` _from Fix variable Z_

### Call 33

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myConsumerOf1#(String(1))
    x# = myConsumerOf2#(String(2))
    x#.hashCode#()
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = myConsumerOf1#(String(1))↩    x# = myConsumerOf2#(String(2))↩    x#.hashCode#()↩}↩_

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

### Call 34

```
myConsumerOf1#(String(1))
```

#### Candidate 1: `FirNamedFunctionSymbol /myConsumerOf1` --- `fun <E5> myConsumerOf1(x: E5): Consumer<E5>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E5)` for `FirNamedFunctionSymbol /myConsumerOf1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E5)` _from Argument String(1)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `Consumer<TypeVariable(E5)> <: TypeVariable(Z)` _from ExpectedType for some call_

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

1. `Consumer<TypeVariable(E5)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 35

```
myConsumerOf2#(String(2))
```

#### Candidate 1: `FirNamedFunctionSymbol /myConsumerOf2` --- `fun <E6> myConsumerOf2(x: E6): Consumer<E6>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(E6)` for `FirNamedFunctionSymbol /myConsumerOf2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(E6)` _from Argument String(2)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `Consumer<TypeVariable(E6)> <: TypeVariable(Z)` _from ExpectedType for some call_

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

1. `Consumer<TypeVariable(E6)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 36

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Consumer<*>`
##### Call Completion:

1. `TypeVariable(Z) == Consumer<*>` _from Fix variable Z_

### Call 33

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = myConsumerOf1#(String(1))
    x# = myConsumerOf2#(String(2))
    x#.hashCode#()
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(E5)` with `Readiness(
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
    2. `TypeVariable(E6)` is `Readiness(
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
3. `TypeVariable(E5) == kotlin/String` _from Fix variable E5_
4. `Consumer<kotlin/String> <: TypeVariable(Z)` _from Fix variable E5_
5. Choose `TypeVariable(E6)` with `Readiness(
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
6. `TypeVariable(E6) == kotlin/String` _from Fix variable E6_
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
8. `TypeVariable(Z) == Consumer<*>` _from Fix variable Z_