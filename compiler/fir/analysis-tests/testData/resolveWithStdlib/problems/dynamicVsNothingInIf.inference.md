## `Source session for module <main>`

### Call 1

```
IllegalStateException#(String(Something is not defined.))
```

#### Candidate 1: `FirConstructorSymbol java/lang/IllegalStateException.IllegalStateException` --- `constructor(p0: String!): {kotlin/IllegalStateException=} IllegalStateException`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String!` _from Argument String(Something is not defined.)_

#### Candidate 2: `FirConstructorSymbol java/lang/IllegalStateException.IllegalStateException` --- `constructor(p0: Throwable!): {kotlin/IllegalStateException=} IllegalStateException`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/Throwable!` _from Argument String(Something is not defined.)_
2. __NewConstraintError: `kotlin/String <: kotlin/Throwable!`__

#### Candidate 1: `FirConstructorSymbol java/lang/IllegalStateException.IllegalStateException` --- `constructor(p0: String!): {kotlin/IllegalStateException=} IllegalStateException`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `{kotlin/IllegalStateException=} java/lang/IllegalStateException <: kotlin/Throwable` _from ExpectedType for some call_

### Call 2

```
parse#(R|<local>/data|)
```

#### Candidate 1: `FirNamedFunctionSymbol /parse` --- `fun <T> parse(data: String): T`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /parse`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument R|<local>/data|_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(T) <: dynamic` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	false ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   )`

### Call 3

```
when () {
    ==(this@R|/Test|.R|/Test.something|, Null(null)) ->  {
        throw R|java/lang/IllegalStateException.IllegalStateException|(String(Something is not defined.))
    }
    else ->  {
        R?C|/parse|(R|<local>/data|)
    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing <: TypeVariable(K)` _from Argument throw R|java/lang/IllegalStateException.IllegalStateException|(String(Something is not defined.))_
2. `TypeVariable(T) <: TypeVariable(K)` _from Argument R?C|/parse|(R|<local>/data|)_

##### Resolution Stages > CheckIncompatibleTypeVariableUpperBounds:

1. `TypeVariable(K) == dynamic` _from ExpectedType for some call_
2. Combine `TypeVariable(T) <: TypeVariable(K)` with `TypeVariable(K) == dynamic`
    1. `TypeVariable(T) <: dynamic`

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
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
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
       	false REIFIED
       	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       )`
2. `TypeVariable(K) == dynamic` _from Fix variable K_
3. Combine `TypeVariable(T) <: TypeVariable(K)` with `TypeVariable(K) == dynamic`
    1. `TypeVariable(T) <: dynamic`
4. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   )`
5. `TypeVariable(T) == dynamic` _from Fix variable T_

### Call 4

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing↩`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: T` _from ExpectedType for some call_