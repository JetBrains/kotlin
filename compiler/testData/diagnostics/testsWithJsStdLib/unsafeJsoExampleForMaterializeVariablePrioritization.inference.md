## `Source session for module <main>`

### Call 1

```
arraysEqual#(R|<local>/first|, R|<local>/second|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/arraysEqual` --- `fun <T> arraysEqual(first: Array<out T>, second: Array<out T>): Boolean↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/arraysEqual`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Array<CapturedType(*)> <: kotlin/Array<out TypeVariable(T)>` _from Argument R|<local>/first|_
    1. `CapturedType(*) <: TypeVariable(T)`

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
2. `TypeVariable(T) == kotlin/Any?` _from Fix variable T_

### Call 2

```
when () {
    (R|<local>/first| is R|kotlin/Array<*>|) && (R|<local>/second| is R|kotlin/Array<*>|) ->  {
        R|kotlin/arraysEqual|<R|kotlin/Any?|>(R|<local>/first|, R|<local>/second|)
    }
    else ->  {
        ==(R|<local>/first|, R|<local>/second|)
    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Boolean <: TypeVariable(K)` _from Argument R|kotlin/arraysEqual|<R|kotlin/Any?|>(R|<local>/first|, R|<local>/second|)_

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Boolean` _from Fix variable K_

### Call 3

```
assertEquals#(Boolean(true), R|<local>/actual|, R|<local>/message|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/assertEquals` --- `fun <T> assertEquals(expected: T, actual: T, message: String? = ...): Unit↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/assertEquals`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Boolean <: TypeVariable(T)` _from Argument Boolean(true)_
2. `kotlin/String? <: kotlin/String?` _from Argument R|<local>/message|_

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
2. `TypeVariable(T) == kotlin/Boolean` _from Fix variable T_

### Call 4

```
assertEquals#(Boolean(false), R|<local>/actual|, R|<local>/message|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/assertEquals` --- `fun <T> assertEquals(expected: T, actual: T, message: String? = ...): Unit↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/assertEquals`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Boolean <: TypeVariable(T)` _from Argument Boolean(false)_
2. `kotlin/String? <: kotlin/String?` _from Argument R|<local>/message|_

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
2. `TypeVariable(T) == kotlin/Boolean` _from Fix variable T_

### Call 5

```
js#(String(({})))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/js/js` --- `fun js(code: String): dynamic↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument String(({}))_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `dynamic <: W` _from ExpectedType for some call_

### Call 6

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing↩`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: R` _from ExpectedType for some call_

### Call 7

```
R|<local>/wrapper|.setState#(setState@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    assign#(unsafeJso#(), it#).apply#(builder#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /ReactComponentWrapper.setState` --- `fun setState(transformState: (S) -> S): Unit↩`
##### Resolution Stages > CheckArguments:

1. `(S) -> S <: (S) -> S` _from Argument setState <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    assign#(unsafeJso#(), it#).apply#(builder#)↩}↩_

### Call 8

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

### Call 9

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

### Call 10

```
R|/assign|<R|S|, R|S|>(R|/unsafeJso|<R|S|>(), vararg(R|<local>/it|)).apply#(R|<local>/builder|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/apply` --- `@InlineOnly() @IgnorableReturnValue() fun <T> T.apply(block: T.() -> Unit): T↩    [R|Contract description]↩     <↩        CallsInPlace(block, EXACTLY_ONCE)↩    >↩↩`
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

### Call 7

```
R|<local>/wrapper|.setState#(setState@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    assign#(unsafeJso#(), it#).apply#(builder#)
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol /ReactComponentWrapper.setState` --- `fun setState(transformState: (S) -> S): Unit↩`
##### Continue Call Completion:

1. `S <: S` _from LambdaArgument_

### Call 11

```
Throwable#(R|<local>/message|)
```

#### Candidate 1: `FirConstructorSymbol kotlin/Throwable.Throwable` --- `constructor(message: String?): Throwable`
##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: kotlin/String?` _from Argument R|<local>/message|_

#### Candidate 2: `FirConstructorSymbol kotlin/Throwable.Throwable` --- `constructor(cause: Throwable?): Throwable`
##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: kotlin/Throwable?` _from Argument R|<local>/message|_
2. __NewConstraintError: `kotlin/String? <: kotlin/Throwable?`__

#### Candidate 1: `FirConstructorSymbol kotlin/Throwable.Throwable` --- `constructor(message: String?): Throwable`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Throwable <: kotlin/Throwable` _from ExpectedType for some call_

### Call 12

```
arraysEqual#(R|<local>/expected|, R|<local>/actual|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/arraysEqual` --- `fun <T> arraysEqual(first: Array<out T>, second: Array<out T>): Boolean↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol kotlin/arraysEqual`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Array<CapturedType(out T)> <: kotlin/Array<out TypeVariable(T)>` _from Argument R|<local>/expected|_
    1. `CapturedType(out T) <: TypeVariable(T)`

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
2. `TypeVariable(T) == T` _from Fix variable T_

### Call 13

```
R|kotlin/arraysEqual|<R|T|>(R|<local>/expected|, R|<local>/actual|).not#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/Boolean.not` --- `@IntrinsicConstEvaluation() fun not(): Boolean↩`
##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Boolean <: kotlin/Boolean` _from ExpectedType for some call_

### Call 14

```
when () {
    ==(R|<local>/message|, Null(null)) ->  {
        String()
    }
    else ->  {
        <strcat>(String(, message = '), R|<local>/message|, String('))
    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(K)` _from Argument String()_

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/String` _from Fix variable K_

### Call 15

```
fail#(<strcat>(String(Unexpected array: expected = '), R|<local>/expected|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/fail` --- `fun fail(message: String? = ...): Nothing↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String?` _from Argument <strcat>(String(Unexpected array: expected = '), R|<local>/expected|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|)_

### Call 16

```
R|<local>/first|.R|SubstitutionOverride<kotlin/Array.size: R|kotlin/Int|>|.minus#(IntegerLiteral(1))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/Int.minus` --- `@IntrinsicConstEvaluation() fun minus(other: Byte): Int↩`
##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: kotlin/Byte` _from Argument IntegerLiteral(1)_

#### Candidate 2: `FirNamedFunctionSymbol kotlin/Int.minus` --- `@IntrinsicConstEvaluation() fun minus(other: Short): Int↩`
##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: kotlin/Short` _from Argument IntegerLiteral(1)_

#### Candidate 3: `FirNamedFunctionSymbol kotlin/Int.minus` --- `@IntrinsicConstEvaluation() fun minus(other: Int): Int↩`
##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: kotlin/Int` _from Argument IntegerLiteral(1)_

#### Candidate 4: `FirNamedFunctionSymbol kotlin/Int.minus` --- `@IntrinsicConstEvaluation() fun minus(other: Long): Long↩`
##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: kotlin/Long` _from Argument IntegerLiteral(1)_

#### Candidate 5: `FirNamedFunctionSymbol kotlin/Int.minus` --- `@IntrinsicConstEvaluation() fun minus(other: Float): Float↩`
##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: kotlin/Float` _from Argument IntegerLiteral(1)_
2. __NewConstraintError: `ILT: 1 <: kotlin/Float`__

#### Candidate 6: `FirNamedFunctionSymbol kotlin/Int.minus` --- `@IntrinsicConstEvaluation() fun minus(other: Double): Double↩`
##### Resolution Stages > CheckArguments:

1. `ILT: 1 <: kotlin/Double` _from Argument IntegerLiteral(1)_
2. __NewConstraintError: `ILT: 1 <: kotlin/Double`__

### Call 17

```
IntegerLiteral(0).rangeTo#(R|<local>/first|.R|SubstitutionOverride<kotlin/Array.size: R|kotlin/Int|>|.R|kotlin/Int.minus|(Int(1)))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/Int.rangeTo` --- `fun rangeTo(other: Byte): IntRange↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Byte` _from Argument R|<local>/first|.R|SubstitutionOverride<kotlin/Array.size: R|kotlin/Int|>|.R|kotlin/Int.minus|(Int(1))_
2. __NewConstraintError: `kotlin/Int <: kotlin/Byte`__

#### Candidate 2: `FirNamedFunctionSymbol kotlin/Int.rangeTo` --- `fun rangeTo(other: Short): IntRange↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Short` _from Argument R|<local>/first|.R|SubstitutionOverride<kotlin/Array.size: R|kotlin/Int|>|.R|kotlin/Int.minus|(Int(1))_
2. __NewConstraintError: `kotlin/Int <: kotlin/Short`__

#### Candidate 3: `FirNamedFunctionSymbol kotlin/Int.rangeTo` --- `fun rangeTo(other: Int): IntRange↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Int` _from Argument R|<local>/first|.R|SubstitutionOverride<kotlin/Array.size: R|kotlin/Int|>|.R|kotlin/Int.minus|(Int(1))_

#### Candidate 4: `FirNamedFunctionSymbol kotlin/Int.rangeTo` --- `fun rangeTo(other: Long): LongRange↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Long` _from Argument R|<local>/first|.R|SubstitutionOverride<kotlin/Array.size: R|kotlin/Int|>|.R|kotlin/Int.minus|(Int(1))_
2. __NewConstraintError: `kotlin/Int <: kotlin/Long`__

### Call 18

```
R|<local>/<iterator>|.hasNext#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/collections/IntIterator.hasNext` --- `fun hasNext(): Boolean↩`
##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Boolean <: kotlin/Boolean` _from ExpectedType for some call_

### Call 19

```
R|<local>/first|.get#(R|<local>/index|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/Array.get` --- `fun get(index: Int): CapturedType(out T)↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Int` _from Argument R|<local>/index|_

### Call 20

```
R|<local>/second|.get#(R|<local>/index|)
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/Array.get` --- `fun get(index: Int): CapturedType(out T)↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Int <: kotlin/Int` _from Argument R|<local>/index|_

### Call 21

```
equal#(R|<local>/first|.R|SubstitutionOverride<kotlin/Array.get: R|CapturedType(out T)|>|(R|<local>/index|), R|<local>/second|.R|SubstitutionOverride<kotlin/Array.get: R|CapturedType(out T)|>|(R|<local>/index|))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/equal` --- `fun equal(first: Any?, second: Any?): Boolean↩`
##### Resolution Stages > CheckArguments:

1. `CapturedType(out T) <: kotlin/Any?` _from Argument R|<local>/first|.R|SubstitutionOverride<kotlin/Array.get: R|CapturedType(out T)|>|(R|<local>/index|)_

### Call 22

```
R|kotlin/equal|(R|<local>/first|.R|SubstitutionOverride<kotlin/Array.get: R|CapturedType(out T)|>|(R|<local>/index|), R|<local>/second|.R|SubstitutionOverride<kotlin/Array.get: R|CapturedType(out T)|>|(R|<local>/index|)).not#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/Boolean.not` --- `@IntrinsicConstEvaluation() fun not(): Boolean↩`
##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Boolean <: kotlin/Boolean` _from ExpectedType for some call_

### Call 23

```
when () {
    ==(R|<local>/message|, Null(null)) ->  {
        String()
    }
    else ->  {
        <strcat>(String(, message = '), R|<local>/message|, String('))
    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(K)` _from Argument String()_

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/String` _from Fix variable K_

### Call 24

```
fail#(<strcat>(String(Unexpected value: expected = '), R|<local>/expected|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/fail` --- `fun fail(message: String? = ...): Nothing↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String?` _from Argument <strcat>(String(Unexpected value: expected = '), R|<local>/expected|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|)_

### Call 25

```
when () {
    ==(R|<local>/message|, Null(null)) ->  {
        String()
    }
    else ->  {
        <strcat>(String(, message = '), R|<local>/message|, String('))
    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(K)` _from Argument String()_

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/String` _from Fix variable K_

### Call 26

```
fail#(<strcat>(String(Illegal value: illegal = '), R|<local>/illegal|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/fail` --- `fun fail(message: String? = ...): Nothing↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String?` _from Argument <strcat>(String(Illegal value: illegal = '), R|<local>/illegal|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|)_

### Call 27

```
when () {
    ==(R|<local>/message|, Null(null)) ->  {
        String()
    }
    else ->  {
        <strcat>(String(, message = '), R|<local>/message|, String('))
    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(K)` _from Argument String()_

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/String` _from Fix variable K_

### Call 28

```
fail#(<strcat>(String(Expected same instances: expected = '), R|<local>/expected|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/fail` --- `fun fail(message: String? = ...): Nothing↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String?` _from Argument <strcat>(String(Expected same instances: expected = '), R|<local>/expected|, String(', actual = '), R|<local>/actual|, String('), R|<local>/msg|)_

### Call 29

```
assertTrue#(R|<local>/f|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Boolean|>|(), R|<local>/f|.R|kotlin/Any.toString|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/assertTrue` --- `fun assertTrue(actual: Boolean, message: String? = ...): Unit↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Boolean <: kotlin/Boolean` _from Argument R|<local>/f|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Boolean|>|()_
2. `kotlin/String <: kotlin/String?` _from Argument R|<local>/f|.R|kotlin/Any.toString|()_

### Call 30

```
assertFalse#(R|<local>/f|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Boolean|>|(), R|<local>/f|.R|kotlin/Any.toString|())
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/assertFalse` --- `fun assertFalse(actual: Boolean, message: String? = ...): Unit↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/Boolean <: kotlin/Boolean` _from Argument R|<local>/f|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Boolean|>|()_
2. `kotlin/String <: kotlin/String?` _from Argument R|<local>/f|.R|kotlin/Any.toString|()_

### Call 31

```
try {
    R|<local>/block|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Unit|>|()
}
catch (t: R|kotlin/Throwable|) {
    ^assertFails R|<local>/t|
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/TRY_CALL` --- `fun <K> TRY_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/TRY_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Unit <: TypeVariable(K)` _from Argument R|<local>/block|.R|SubstitutionOverride<kotlin/Function0.invoke: R|kotlin/Unit|>|()_
2. `kotlin/Nothing <: TypeVariable(K)` _from Argument ^assertFails R|<local>/t|_

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
   	 true HAS_PROPER_NON_NOTHING_NON_ILT_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Unit` _from Fix variable K_

### Call 32

```
fail#(String(Expected an exception to be thrown, but was completed successfully.))
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/fail` --- `fun fail(message: String? = ...): Nothing↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String?` _from Argument String(Expected an exception to be thrown, but was completed successfully.)_