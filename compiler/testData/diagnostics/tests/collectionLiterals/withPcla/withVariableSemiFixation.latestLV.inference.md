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
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x#.size#↩}↩_

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

### Call 3

```
Q|kotlin/collections|.listOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/listOf|(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))_

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

### Call 4

```
<collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 5

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: List<Int>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/List<TypeVariable(T)>` _from Fix variable Z_

### Call 2

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(T) == kotlin/Int` _from Fix variable T_
4. `kotlin/collections/List<kotlin/Int> <: TypeVariable(Z)` _from Fix variable T_
5. `TypeVariable(Z) == kotlin/collections/List<kotlin/Int>` _from Fix variable T_
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

### Call 6

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x#.size#↩}↩_

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

### Call 7

```
setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 8

```
Q|kotlin/collections|.setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/setOf|(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))_

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

### Call 9

```
<collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 10

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<Int>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(T) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `ILT: 1 <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `ILT: 1 <: TypeVariable(_CST_0)`
4. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 11

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 12

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x# = <collectionLiteralCall>(String(!))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x# = <collectionLiteralCall>(String(!))↩    x#.size#↩}↩_

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

### Call 13

```
setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 14

```
Q|kotlin/collections|.setOf#(String(!))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(element: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 14

```
Q|kotlin/collections|.setOf#(String(!))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(element: T): Set<T>`
##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/setOf|(String(!))_

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(!)_

### Call 15

```
<collectionLiteralCall>(String(!))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 16

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Set<it(Comparable<*> & Serializable)>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(T) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `ILT: 1 <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `ILT: 1 <: TypeVariable(_CST_0)`
4. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
5. `TypeVariable(Z) == kotlin/collections/Set<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 17

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

### Call 18

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = <collectionLiteralCall>(String(!))
    x# = setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = <collectionLiteralCall>(String(!))↩    x# = setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x#.size#↩}↩_

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

### Call 19

```
Q|kotlin/collections|.listOf#(String(!))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

#### Candidate 2: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

##### Some compareCallsByUsedArguments() call:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0
2. `TypeVariable(T) <: kotlin/Any?` _from SimpleConstraintSystemConstraintPosition_
3. `T <: TypeVariable(T)` _from SimpleConstraintSystemConstraintPosition_

### Call 19

```
Q|kotlin/collections|.listOf#(String(!))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(element: T): List<T>`
##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/listOf|(String(!))_

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(!)_

### Call 20

```
<collectionLiteralCall>(String(!))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 21

```
setOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/setOf` --- `fun <T> setOf(vararg elements: T): Set<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/setOf`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/Set<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 22

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: Collection<it(Comparable<*> & Serializable)>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(T) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
4. Combine `ILT: 1 <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `ILT: 1 <: TypeVariable(_CST_0)`
5. `TypeVariable(Z) == kotlin/collections/Collection<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 23

```
this@R|special/anonymous|.R?C|/Box.x|.size#
```

#### Candidate 1: `FirRegularPropertySymbol kotlin/collections/Collection.size` --- `val size: Int`
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

### Call 24

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x# = <collectionLiteralCall>(String(1), String(2), String(3))
    x#.size#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x# = <collectionLiteralCall>(String(1), String(2), String(3))↩    x#.size#↩}↩_

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

### Call 25

```
Q|kotlin/collections|.listOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/listOf|(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))_

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

### Call 26

```
<collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 27

```
Q|kotlin/collections|.listOf#(String(1), String(2), String(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/listOf|(String(1), String(2), String(3))_

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(1)_

### Call 28

```
<collectionLiteralCall>(String(1), String(2), String(3))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 29

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: List<it(Comparable<*> & Serializable)>`
##### Call Completion:

1. New `TypeVariable(_CST_0)`
2. `TypeVariable(T) <: TypeVariable(_CST_0)` _from Synthetic CST variable bound_
3. Combine `ILT: 1 <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `ILT: 1 <: TypeVariable(_CST_0)`
4. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(_CST_0)`
    1. `kotlin/String <: TypeVariable(_CST_0)`
5. `TypeVariable(Z) == kotlin/collections/List<TypeVariable(_CST_0)>` _from Fix variable Z_

### Call 30

```
this@R|special/anonymous|.R?C|/Box.x|.size#
```

#### Candidate 1: `FirRegularPropertySymbol kotlin/collections/List.size` --- `val size: Int`
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

### Call 31

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x#.size#
    x# = <collectionLiteralCall>(String(1), String(2), String(3))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(Z)` for `FirNamedFunctionSymbol /buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(Z)>.() -> kotlin/Unit <: Box<TypeVariable(Z)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))↩    x#.size#↩    x# = <collectionLiteralCall>(String(1), String(2), String(3))↩}↩_

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

### Call 32

```
Q|kotlin/collections|.listOf#(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/listOf|(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))_

##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: TypeVariable(T)` _from Argument IntegerLiteral(1)_
2. `ILT: 2 <: TypeVariable(T)` _from Argument IntegerLiteral(2)_
3. `ILT: 3 <: TypeVariable(T)` _from Argument IntegerLiteral(3)_

### Call 33

```
<collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

##### Some addSubtypeConstraintIfCompatible() with currentCommonSystem inside PCLA inference session:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 34

```
x#
```

#### Candidate 1: `FirRegularPropertySymbol /Box.x` --- `var x: List<it(Comparable<*> & Serializable)>`
##### Call Completion:

1. `TypeVariable(Z) == kotlin/collections/List<TypeVariable(T)>` _from Fix variable Z_

### Call 35

```
Q|kotlin/collections|.listOf#(String(1), String(2), String(3))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/listOf` --- `fun <T> listOf(vararg elements: T): List<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/collections/listOf`s parameter 0

##### Continue Resolution Stages > CheckLowPriorityInOverloadResolution:

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from Argument Q|kotlin/collections|.R?C|kotlin/collections/listOf|(String(1), String(2), String(3))_
2. Combine `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` with `TypeVariable(Z) == kotlin/collections/List<TypeVariable(T)>`
    1. `TypeVariable(T) <: TypeVariable(T)`

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(1)_

### Call 36

```
<collectionLiteralCall>(String(1), String(2), String(3))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ACCEPT_SPECIFIC_TYPE_CALL` --- `fun ACCEPT_SPECIFIC_TYPE_CALL(reference: TypeVariable(Z)): Unit`
##### Continue Call Completion:

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

1. `kotlin/collections/List<TypeVariable(T)> <: TypeVariable(Z)` _from ExpectedType for some call_

### Call 31

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    x# = <collectionLiteralCall>(IntegerLiteral(1), IntegerLiteral(2), IntegerLiteral(3))
    x#.size#
    x# = <collectionLiteralCall>(String(1), String(2), String(3))
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /buildBox` --- `fun <Z> buildBox(block: Box<Z>.() -> Unit): Box<Z>`
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
    1. `TypeVariable(Z)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(T)` is `Readiness(
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
3. `TypeVariable(T) == it(kotlin/Comparable<*> & java/io/Serializable)` _from Fix variable T_
4. Combine `TypeVariable(T) <: TypeVariable(T)` with `TypeVariable(T) == it(kotlin/Comparable<*> & java/io/Serializable)`
    1. `TypeVariable(T) <: kotlin/Comparable<*>`
    2. `TypeVariable(T) <: java/io/Serializable`
    3. `TypeVariable(T) <: it(kotlin/Comparable<*> & java/io/Serializable)`
5. `kotlin/collections/List<it(kotlin/Comparable<*> & java/io/Serializable)> <: TypeVariable(Z)` _from Fix variable T_
6. `TypeVariable(Z) == kotlin/collections/List<it(kotlin/Comparable<*> & java/io/Serializable)>` _from Fix variable T_
7. Choose `TypeVariable(T)` with `Readiness(
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
8. `TypeVariable(T) == kotlin/String` _from Fix variable T_
9. `kotlin/collections/List<kotlin/String> <: TypeVariable(Z)` _from Fix variable T_
10. Choose `TypeVariable(Z)` with `Readiness(
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
