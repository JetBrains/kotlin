## `Source session for module <main>`

### Call 1

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing↩`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: W` _from ExpectedType for some call_

### Call 2

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing↩`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: R` _from ExpectedType for some call_

### Call 3

```
R|<local>/wrapper|.setState#(setState@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    assign#(unsafeJso#(), it#).apply#(builder#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /ReactComponentWrapper.setState` --- `fun setState(transformState: (S) -> S): Unit↩`
##### Resolution Stages > CheckArguments:

1. `(S) -> S <: (S) -> S` _from Argument setState <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    assign#(unsafeJso#(), it#).apply#(builder#)↩}↩_

### Call 4

```
unsafeJso#()
```

#### Candidate 1: `FirNamedFunctionSymbol /unsafeJso` --- `fun <W : Any> unsafeJso(): W↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(W)` for `FirNamedFunctionSymbol /unsafeJso`s parameter 0
2. `TypeVariable(W) <: kotlin/Any` _from DeclaredUpperBound_

##### Call Completion:

1. Choose `TypeVariable(W)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`

### Call 5

```
assign#(R?C|/unsafeJso|(), R|<local>/it|)
```

#### Candidate 1: `FirNamedFunctionSymbol /assign` --- `fun <T : Any, R : T> assign(dest: R, vararg src: T?): R↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /assign`s parameter 0
2. New `TypeVariable(R)` for `FirNamedFunctionSymbol /assign`s parameter 1
3. `TypeVariable(T) <: kotlin/Any` _from DeclaredUpperBound_
4. `TypeVariable(R) <: TypeVariable(T)` _from DeclaredUpperBound_
5. Combine `TypeVariable(R) <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Any`
    1. `TypeVariable(R) <: kotlin/Any`

##### Resolution Stages > CheckArguments:

1. `TypeVariable(W) <: TypeVariable(R)` _from Argument R?C|/unsafeJso|()_
2. Combine `TypeVariable(W) <: TypeVariable(R)` with `TypeVariable(R) <: TypeVariable(T)`
    1. `TypeVariable(W) <: TypeVariable(T)`
3. Combine `TypeVariable(W) <: TypeVariable(R)` with `TypeVariable(R) <: kotlin/Any`
    1. `TypeVariable(W) <: kotlin/Any`
4. `S <: TypeVariable(T)?` _from Argument R|<local>/it|_
    1. `S <: TypeVariable(T)`

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
    1. `TypeVariable(R)` is `Readiness(
       	 true ALLOWED
       	false HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       )`
    2. `TypeVariable(W)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       )`
2. `TypeVariable(T) == S` _from Fix variable T_
3. Combine `TypeVariable(R) <: TypeVariable(T)` with `TypeVariable(T) == S`
    1. `TypeVariable(R) <: S`
4. Combine `TypeVariable(W) <: TypeVariable(T)` with `TypeVariable(T) == S`
    1. `TypeVariable(W) <: S`
5. Choose `TypeVariable(R)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
    1. `TypeVariable(W)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       )`
6. `TypeVariable(R) == S` _from Fix variable R_
7. Choose `TypeVariable(W)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
8. `TypeVariable(W) == S` _from Fix variable W_

### Call 6

```
R|/assign|<R|S|, R|S|>(R|/unsafeJso|<R|S|>(), vararg(R|<local>/it|)).apply#(R|<local>/builder|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/apply` --- `@IgnorableReturnValue() @InlineOnly() fun <T> T.apply(block: T.() -> Unit): T↩    [R|Contract description]↩     <↩        CallsInPlace(block, EXACTLY_ONCE)↩    >↩↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/apply`s parameter 0

##### Resolution Stages > CheckExtensionReceiver:

1. `S <: TypeVariable(T)` _from Receiver R|/assign|<R|S|, R|S|>(R|/unsafeJso|<R|S|>(), vararg(R|<local>/it|))_

##### Resolution Stages > CheckArguments:

1. `S.() -> kotlin/Unit <: TypeVariable(T).() -> kotlin/Unit` _from Argument R|<local>/builder|_
    1. `TypeVariable(T) <: S`
2. Combine `S <: TypeVariable(T)` with `TypeVariable(T) <: S`
    1. `TypeVariable(T) == S`

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `TypeVariable(T) <: S` _from ExpectedType for some call_

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
   	false HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(T) == S` _from Fix variable T_

### Call 3

```
R|<local>/wrapper|.setState#(setState@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    assign#(unsafeJso#(), it#).apply#(builder#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /ReactComponentWrapper.setState` --- `fun setState(transformState: (S) -> S): Unit↩`
##### Continue Call Completion:

1. `S <: S` _from LambdaArgument_
