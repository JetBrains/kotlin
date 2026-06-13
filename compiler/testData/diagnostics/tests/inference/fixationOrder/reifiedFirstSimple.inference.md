## `Source session for module <main>`

### Call 1

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: R` _from ExpectedType for some call_

### Call 2

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Derived` _from ExpectedType for some call_

### Call 3

```
decode#()
```

#### Candidate 1: `FirNamedFunctionSymbol /decode` --- `fun <reified T1 : Any> decode(): T1?`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T1)` for `FirNamedFunctionSymbol /decode`s parameter 0
2. `TypeVariable(T1) <: kotlin/Any` _from DeclaredUpperBound_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(T1)? <: Base?` _from ExpectedType for some call_
    1. `TypeVariable(T1) <: Base?`

##### Call Completion:

1. Choose `TypeVariable(T1)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	 true REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T1) == Base` _from Fix variable T1_

### Call 4

```
R|/decode|<R|Base|>() ?: R|/d|
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL` --- `fun <K> ELVIS_CALL(x: K?, y: K): @Exact K`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Base? <: TypeVariable(K)?` _from Argument R|/decode|<R|Base|>()_
    1. `Base <: TypeVariable(K)`
2. `Derived <: TypeVariable(K)` _from Argument R|/d|_

##### Resolution Stages > CheckIncompatibleTypeVariableUpperBounds:

1. `@Exact TypeVariable(K) <: Base` _from ExpectedType for some call_
    1. `TypeVariable(K) <: Base`
    2. `Base <: TypeVariable(K)`
2. Combine `Base <: TypeVariable(K)` with `TypeVariable(K) <: Base`
    1. `TypeVariable(K) == Base`

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
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
2. `TypeVariable(K) == Base` _from Fix variable K_

### Call 5

```
decodeNonReified#()
```

#### Candidate 1: `FirNamedFunctionSymbol /decodeNonReified` --- `fun <T2 : Any> decodeNonReified(): T2?`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T2)` for `FirNamedFunctionSymbol /decodeNonReified`s parameter 0
2. `TypeVariable(T2) <: kotlin/Any` _from DeclaredUpperBound_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(T2)? <: Base?` _from ExpectedType for some call_
    1. `TypeVariable(T2) <: Base?`

##### Call Completion:

1. Choose `TypeVariable(T2)` with `Readiness(
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
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T2) == Base` _from Fix variable T2_

### Call 6

```
R|/decodeNonReified|<R|Base|>() ?: R|/d|
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL` --- `fun <K> ELVIS_CALL(x: K?, y: K): @Exact K`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Base? <: TypeVariable(K)?` _from Argument R|/decodeNonReified|<R|Base|>()_
    1. `Base <: TypeVariable(K)`
2. `Derived <: TypeVariable(K)` _from Argument R|/d|_

##### Resolution Stages > CheckIncompatibleTypeVariableUpperBounds:

1. `@Exact TypeVariable(K) <: Base` _from ExpectedType for some call_
    1. `TypeVariable(K) <: Base`
    2. `Base <: TypeVariable(K)`
2. Combine `Base <: TypeVariable(K)` with `TypeVariable(K) <: Base`
    1. `TypeVariable(K) == Base`

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
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
2. `TypeVariable(K) == Base` _from Fix variable K_

### Call 7

```
myRun#(<L> = myRun@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    decode#() ?: d#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /myRun` --- `fun <R> myRun(x: () -> R): R`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(R)` for `FirNamedFunctionSymbol /myRun`s parameter 0

##### Resolution Stages > CheckArguments:

1. `() -> TypeVariable(R) <: () -> TypeVariable(R)` _from Argument <L> = myRun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    decode#() ?: d#↩}↩_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(R) <: Base` _from ExpectedType for some call_

### Call 8

```
decode#()
```

#### Candidate 1: `FirNamedFunctionSymbol /decode` --- `fun <reified T1 : Any> decode(): T1?`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T1)` for `FirNamedFunctionSymbol /decode`s parameter 0
2. `TypeVariable(T1) <: kotlin/Any` _from DeclaredUpperBound_

##### Call Completion:

1. Choose `TypeVariable(T1)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 9

```
R?C|/decode|() ?: R|/d|
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL` --- `fun <K> ELVIS_CALL(x: K?, y: K): @Exact K`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `TypeVariable(T1)? <: TypeVariable(K)?` _from Argument R?C|/decode|()_
    1. `TypeVariable(T1) & Any <: TypeVariable(K)`
    2. `TypeVariable(T1) <: TypeVariable(K)?`
2. `Derived <: TypeVariable(K)` _from Argument R|/d|_

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(T1)` is `Readiness(
       	false ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`

### Call 7

```
myRun#(<L> = myRun@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    decode#() ?: d#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /myRun` --- `fun <R> myRun(x: () -> R): R`
##### Continue Call Completion:

1. `@Exact TypeVariable(K) <: TypeVariable(R)` _from LambdaArgument_
    1. `TypeVariable(K) <: TypeVariable(R)`
    2. `TypeVariable(R) <: TypeVariable(K)`
2. Combine `TypeVariable(K) <: TypeVariable(R)` with `TypeVariable(R) <: Base`
    1. `TypeVariable(K) <: Base`
3. Combine `TypeVariable(T1) & Any <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(R)`
    1. `TypeVariable(T1) & Any <: TypeVariable(R)`
    2. `TypeVariable(T1) <: TypeVariable(R)?`
4. Combine `Derived <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(R)`
    1. `Derived <: TypeVariable(R)`
5. Combine `TypeVariable(K) <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(K)`
    1. `TypeVariable(K) == TypeVariable(R)`
6. Combine `TypeVariable(K) <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(K)`
    1. `TypeVariable(R) == TypeVariable(K)`
7. Combine `TypeVariable(T1) & Any <: TypeVariable(K)` with `TypeVariable(K) <: Base`
    1. `TypeVariable(T1) <: Base?`
8. Combine `TypeVariable(K) == TypeVariable(R)` with `TypeVariable(K) <: Base`
    1. `TypeVariable(R) <: Base`
9. Combine `TypeVariable(T1) & Any <: TypeVariable(R)` with `TypeVariable(R) == TypeVariable(K)`
    1. `TypeVariable(T1) & Any <: TypeVariable(K)`
    2. `TypeVariable(T1) <: TypeVariable(K)?`
10. Combine `Derived <: TypeVariable(R)` with `TypeVariable(R) == TypeVariable(K)`
    1. `Derived <: TypeVariable(K)`
11. Choose `TypeVariable(T1)` with `Readiness(
    	 true ALLOWED
    	 true HAS_PROPER_CONSTRAINTS
    	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
    	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
    	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
    	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
    	 true REIFIED
    	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
    	 true HAS_PROPER_NON_ILT_CONSTRAINT
    	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
    	false HAS_PROPER_EQUALITY_CONSTRAINT
    	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
    )`
    1. `TypeVariable(R)` is `Readiness(
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
    2. `TypeVariable(K)` is `Readiness(
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
12. `TypeVariable(T1) == Base` _from Fix variable T1_
13. Combine `TypeVariable(T1) == Base` with `TypeVariable(T1) <: TypeVariable(K)?`
    1. `Base <: TypeVariable(K)`
14. Combine `TypeVariable(T1) == Base` with `TypeVariable(T1) <: TypeVariable(R)?`
    1. `Base <: TypeVariable(R)`
15. Combine `TypeVariable(K) <: Base` with `Base <: TypeVariable(K)`
    1. `TypeVariable(K) == Base`
16. Combine `TypeVariable(R) <: Base` with `Base <: TypeVariable(R)`
    1. `TypeVariable(R) == Base`
17. Choose `TypeVariable(R)` with `Readiness(
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
    1. `TypeVariable(K)` is `Readiness(
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
18. Choose `TypeVariable(K)` with `Readiness(
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

### Call 10

```
myRun#(<L> = myRun@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    decodeNonReified#() ?: d#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /myRun` --- `fun <R> myRun(x: () -> R): R`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(R)` for `FirNamedFunctionSymbol /myRun`s parameter 0

##### Resolution Stages > CheckArguments:

1. `() -> TypeVariable(R) <: () -> TypeVariable(R)` _from Argument <L> = myRun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    decodeNonReified#() ?: d#↩}↩_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(R) <: Base` _from ExpectedType for some call_

### Call 11

```
decodeNonReified#()
```

#### Candidate 1: `FirNamedFunctionSymbol /decodeNonReified` --- `fun <T2 : Any> decodeNonReified(): T2?`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T2)` for `FirNamedFunctionSymbol /decodeNonReified`s parameter 0
2. `TypeVariable(T2) <: kotlin/Any` _from DeclaredUpperBound_

##### Call Completion:

1. Choose `TypeVariable(T2)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 12

```
R?C|/decodeNonReified|() ?: R|/d|
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL` --- `fun <K> ELVIS_CALL(x: K?, y: K): @Exact K`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/ELVIS_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `TypeVariable(T2)? <: TypeVariable(K)?` _from Argument R?C|/decodeNonReified|()_
    1. `TypeVariable(T2) & Any <: TypeVariable(K)`
    2. `TypeVariable(T2) <: TypeVariable(K)?`
2. `Derived <: TypeVariable(K)` _from Argument R|/d|_

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(T2)` is `Readiness(
       	false ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`

### Call 10

```
myRun#(<L> = myRun@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    decodeNonReified#() ?: d#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /myRun` --- `fun <R> myRun(x: () -> R): R`
##### Continue Call Completion:

1. `@Exact TypeVariable(K) <: TypeVariable(R)` _from LambdaArgument_
    1. `TypeVariable(K) <: TypeVariable(R)`
    2. `TypeVariable(R) <: TypeVariable(K)`
2. Combine `TypeVariable(K) <: TypeVariable(R)` with `TypeVariable(R) <: Base`
    1. `TypeVariable(K) <: Base`
3. Combine `TypeVariable(T2) & Any <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(R)`
    1. `TypeVariable(T2) & Any <: TypeVariable(R)`
    2. `TypeVariable(T2) <: TypeVariable(R)?`
4. Combine `Derived <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(R)`
    1. `Derived <: TypeVariable(R)`
5. Combine `TypeVariable(K) <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(K)`
    1. `TypeVariable(K) == TypeVariable(R)`
6. Combine `TypeVariable(K) <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(K)`
    1. `TypeVariable(R) == TypeVariable(K)`
7. Combine `TypeVariable(T2) & Any <: TypeVariable(K)` with `TypeVariable(K) <: Base`
    1. `TypeVariable(T2) <: Base?`
8. Combine `TypeVariable(K) == TypeVariable(R)` with `TypeVariable(K) <: Base`
    1. `TypeVariable(R) <: Base`
9. Combine `TypeVariable(T2) & Any <: TypeVariable(R)` with `TypeVariable(R) == TypeVariable(K)`
    1. `TypeVariable(T2) & Any <: TypeVariable(K)`
    2. `TypeVariable(T2) <: TypeVariable(K)?`
10. Combine `Derived <: TypeVariable(R)` with `TypeVariable(R) == TypeVariable(K)`
    1. `Derived <: TypeVariable(K)`
11. Choose `TypeVariable(R)` with `Readiness(
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
    1. `TypeVariable(K)` is `Readiness(
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
    2. `TypeVariable(T2)` is `Readiness(
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
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
12. `TypeVariable(R) == Derived` _from Fix variable R_
13. Combine `TypeVariable(R) == TypeVariable(K)` with `TypeVariable(R) == Derived`
    1. `TypeVariable(K) <: Derived`
14. Combine `TypeVariable(T2) & Any <: TypeVariable(R)` with `TypeVariable(R) == Derived`
    1. `TypeVariable(T2) <: Derived?`
15. Combine `Derived <: TypeVariable(K)` with `TypeVariable(K) <: Derived`
    1. `TypeVariable(K) == Derived`
16. Combine `TypeVariable(K) == TypeVariable(R)` with `TypeVariable(K) == Derived`
    1. `TypeVariable(R) <: Derived`
17. Choose `TypeVariable(K)` with `Readiness(
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
    1. `TypeVariable(T2)` is `Readiness(
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
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
18. Choose `TypeVariable(T2)` with `Readiness(
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
    	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
    )`
19. `TypeVariable(T2) == Derived` _from Fix variable T2_
