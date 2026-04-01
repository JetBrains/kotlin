## `Source session for module <main>`

### Call 1

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing↩`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: P` _from ExpectedType for some call_

### Call 2

```
ifTrue#(R|<local>/flag|, <L> = ifTrue@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    try {
        parse#(decode#(token#))
    }
    catch (e: Exception) {
        Null(null)
    }

}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /ifTrue` --- `fun <T : Any> ifTrue(condition: Boolean, exec: () -> T?): T?`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /ifTrue`s parameter 0
2. `TypeVariable(T) <: kotlin/Any` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `kotlin/Boolean <: kotlin/Boolean` _from Argument R|<local>/flag|_
2. `() -> TypeVariable(T)? <: () -> TypeVariable(T)?` _from Argument <L> = ifTrue <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    try {↩        parse#(decode#(token#))↩    }↩    catch (e: Exception) {↩        Null(null)↩    }↩↩}↩_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(T)? <: Result?` _from ExpectedType for some call_
    1. `TypeVariable(T) <: Result?`

### Call 3

```
decode#(R|<local>/token|)
```

#### Candidate 1: `FirNamedFunctionSymbol /decode` --- `fun decode(src: String): String`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument R|<local>/token|_

### Call 4

```
parse#(R|/decode|(R|<local>/token|))
```

#### Candidate 1: `FirNamedFunctionSymbol /parse` --- `fun <reified P : Any> parse(text: String): P`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(P)` for `FirNamedFunctionSymbol /parse`s parameter 0
2. `TypeVariable(P) <: kotlin/Any` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument R|/decode|(R|<local>/token|)_

##### Call Completion:

1. Choose `TypeVariable(P)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 5

```
try {
    R?C|/parse|(R|/decode|(R|<local>/token|))
}
catch (e: R|{kotlin/Exception=} java/lang/Exception|) {
    Null(null)
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/TRY_CALL` --- `fun <K> TRY_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/TRY_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `TypeVariable(P) <: TypeVariable(K)` _from Argument R?C|/parse|(R|/decode|(R|<local>/token|))_
2. `kotlin/Nothing? <: TypeVariable(K)` _from Argument Null(null)_

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(P)` is `Readiness(
       	false ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`

### Call 2

```
ifTrue#(R|<local>/flag|, <L> = ifTrue@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    try {
        parse#(decode#(token#))
    }
    catch (e: Exception) {
        Null(null)
    }

}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /ifTrue` --- `fun <T : Any> ifTrue(condition: Boolean, exec: () -> T?): T?`
##### Continue Call Completion:

1. `TypeVariable(K) <: TypeVariable(T)?` _from LambdaArgument_
    1. `TypeVariable(K) & Any <: TypeVariable(T)`
2. Combine `TypeVariable(K) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Any`
    1. `TypeVariable(K) <: kotlin/Any?`
3. Combine `TypeVariable(K) & Any <: TypeVariable(T)` with `TypeVariable(T) <: Result?`
    1. `TypeVariable(K) <: Result?`
4. Combine `TypeVariable(P) <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(T)?`
    1. `TypeVariable(P) & Any <: TypeVariable(T)`
    2. `TypeVariable(P) <: TypeVariable(T)?`
5. Combine `kotlin/Nothing? <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(T)?`
    1. `kotlin/Nothing <: TypeVariable(T)`
6. Combine `TypeVariable(P) <: TypeVariable(K)` with `TypeVariable(K) <: kotlin/Any?`
    1. `TypeVariable(P) <: kotlin/Any?`
7. Combine `TypeVariable(P) <: TypeVariable(K)` with `TypeVariable(K) <: Result?`
    1. `TypeVariable(P) <: Result?`
8. Choose `TypeVariable(P)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(T)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
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
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
9. `TypeVariable(P) == Result` _from Fix variable P_
10. Combine `TypeVariable(P) == Result` with `TypeVariable(P) <: TypeVariable(K)`
    1. `Result <: TypeVariable(K)`
11. Combine `TypeVariable(P) == Result` with `TypeVariable(P) <: TypeVariable(T)?`
    1. `Result <: TypeVariable(T)`
12. Choose `TypeVariable(T)` with `Readiness(
    	 true ALLOWED
    	 true HAS_PROPER_CONSTRAINTS
    	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
    	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
    	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
    	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
    	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
    	 true HAS_PROPER_NON_ILT_CONSTRAINT
    	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
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
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
13. `TypeVariable(T) == Result` _from Fix variable T_
14. Choose `TypeVariable(K)` with `Readiness(
    	 true ALLOWED
    	 true HAS_PROPER_CONSTRAINTS
    	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
    	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
    	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
    	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
    	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
    	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
    	 true HAS_PROPER_NON_ILT_CONSTRAINT
    	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
    	false HAS_PROPER_EQUALITY_CONSTRAINT
    	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
    )`
15. `TypeVariable(K) == Result?` _from Fix variable K_