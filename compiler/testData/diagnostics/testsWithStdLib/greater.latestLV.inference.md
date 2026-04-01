## `Source session for module <main>`

### Call 1

```
Expression#(R|<local>/other|)
```

#### Candidate 1: `FirConstructorSymbol /Expression.Expression` --- `constructor<M>(x: M): Expression<M>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(M)` for `FirRegularClassSymbol Expression`s parameter 0

##### Resolution Stages > CheckArguments:

1. `T <: TypeVariable(M)` _from Argument R|<local>/other|_

##### Call Completion:

1. Choose `TypeVariable(M)` with `Readiness(
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
GreaterOp#(this@R|/greater|, R?C|/Expression.Expression|(R|<local>/other|))
```

#### Candidate 1: `FirConstructorSymbol /GreaterOp.GreaterOp` --- `constructor(expr1: Expression<*>, expr2: Expression<*>): GreaterOp`
##### Resolution Stages > CheckArguments:

1. `Expression<CapturedType(in S)> <: Expression<*>` _from Argument this|/greater|_
2. `Expression<TypeVariable(M)> <: Expression<*>` _from Argument R?C|/Expression.Expression|(R|<local>/other|)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `GreaterOp <: GreaterOp` _from ExpectedType for some call_

##### Call Completion:

1. Choose `TypeVariable(M)` with `Readiness(
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
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(M) == T` _from Fix variable M_

### Call 3

```
R|<local>/countExpr|.greater#(IntegerLiteral(0))
```

#### Candidate 1: `FirNamedFunctionSymbol /greater` --- `fun <T : Comparable<T>, S : T?> Expression<in S>.greater(other: T): GreaterOp`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /greater`s parameter 0
2. New `TypeVariable(S)` for `FirNamedFunctionSymbol /greater`s parameter 1
3. `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>` _from DeclaredUpperBound_
4. `TypeVariable(S) <: TypeVariable(T)?` _from DeclaredUpperBound_
    1. `TypeVariable(S) & Any <: TypeVariable(T)`
5. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    2. `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
6. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`

##### Resolution Stages > CheckExtensionReceiver:

1. `Expression<kotlin/Long> <: Expression<in TypeVariable(S)>` _from Receiver R|<local>/countExpr|_
    1. `TypeVariable(S) <: kotlin/Long`

##### Resolution Stages > CheckArguments:

1. `ILT: 0 <: TypeVariable(T)` _from Argument IntegerLiteral(0)_
2. Combine `ILT: 0 <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: ILT: 0`
3. Combine `ILT: 0 <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(S) <: ILT: 0?`
4. Combine `ILT: 0 <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/Comparable<ILT: 0>`
5. Combine `ILT: 0 <: TypeVariable(T)` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(S) <: kotlin/Comparable<ILT: 0>?`
6. Combine `ILT: 0 <: TypeVariable(T)` with `TypeVariable(T) <: ILT: 0`
    1. `TypeVariable(T) == ILT: 0`

##### Call Completion:

1. Choose `TypeVariable(S)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
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
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == kotlin/Long` _from Fix variable S_
3. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: TypeVariable(T)?`
    1. `kotlin/Long <: TypeVariable(T)`
4. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(T) <: kotlin/Long`
5. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Long?`
6. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(T) <: kotlin/Comparable<kotlin/Long>`
7. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/Long>?`
8. Combine `kotlin/Long <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Long`
    1. `TypeVariable(T) == kotlin/Long`
9. Choose `TypeVariable(T)` with `Readiness(
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
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 4

```
R|<local>/countExpr|.greater#(String(0))
```

#### Candidate 1: `FirNamedFunctionSymbol /greater` --- `fun <T : Comparable<T>, S : T?> Expression<in S>.greater(other: T): GreaterOp`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /greater`s parameter 0
2. New `TypeVariable(S)` for `FirNamedFunctionSymbol /greater`s parameter 1
3. `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>` _from DeclaredUpperBound_
4. `TypeVariable(S) <: TypeVariable(T)?` _from DeclaredUpperBound_
    1. `TypeVariable(S) & Any <: TypeVariable(T)`
5. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    2. `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
6. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`

##### Resolution Stages > CheckExtensionReceiver:

1. `Expression<kotlin/Long> <: Expression<in TypeVariable(S)>` _from Receiver R|<local>/countExpr|_
    1. `TypeVariable(S) <: kotlin/Long`

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(0)_
2. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/String`
3. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(S) <: kotlin/String?`
4. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/Comparable<kotlin/String>`
5. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/String>?`
6. Combine `kotlin/String <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/String`
    1. `TypeVariable(T) == kotlin/String`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(S)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T) == kotlin/String` _from Fix variable T_
3. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) == kotlin/String`
    1. `TypeVariable(S) <: kotlin/String?`
4. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/String`
    2. `TypeVariable(T) <: kotlin/Comparable<kotlin/String>`
5. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/String>?`
6. Choose `TypeVariable(S)` with `Readiness(
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
7. __InferredEmptyIntersectionWarning__
8. `TypeVariable(S) == it(kotlin/Long & kotlin/String)` _from Fix variable S_
9. Combine `TypeVariable(S) == it(kotlin/Long & kotlin/String)` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Long?`
    2. `TypeVariable(S) <: kotlin/Comparable<it(kotlin/Long & kotlin/String)>?`

### Call 5

```
R|<local>/countExpr|.greater#<R|kotlin/String|, R|kotlin/Nothing|>(String(0))
```

#### Candidate 1: `FirNamedFunctionSymbol /greater` --- `fun <T : Comparable<T>, S : T?> Expression<in S>.greater(other: T): GreaterOp`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /greater`s parameter 0
2. New `TypeVariable(S)` for `FirNamedFunctionSymbol /greater`s parameter 1
3. `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>` _from DeclaredUpperBound_
4. `TypeVariable(S) <: TypeVariable(T)?` _from DeclaredUpperBound_
    1. `TypeVariable(S) & Any <: TypeVariable(T)`
5. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    2. `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
6. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
7. `TypeVariable(T) == kotlin/String` _from TypeParameter R|kotlin/String|_
8. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) == kotlin/String`
    1. `TypeVariable(S) <: kotlin/String?`
9. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/String`
    2. `TypeVariable(T) <: kotlin/Comparable<kotlin/String>`
10. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/String>?`
11. `TypeVariable(S) == kotlin/Nothing` _from TypeParameter R|kotlin/Nothing|_
12. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(S) <: TypeVariable(T)?`
    1. `kotlin/Nothing <: TypeVariable(T)`
13. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(T) <: kotlin/Comparable<kotlin/Nothing>`
14. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/Nothing>?`

##### Resolution Stages > CheckExtensionReceiver:

1. `Expression<kotlin/Long> <: Expression<in TypeVariable(S)>` _from Receiver R|<local>/countExpr|_
    1. `TypeVariable(S) <: kotlin/Long`

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(T)` _from Argument String(0)_

##### Call Completion:

1. Choose `TypeVariable(S)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(T)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == kotlin/Nothing` _from Fix variable S_
3. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(S) <: TypeVariable(T)?`
    1. `kotlin/Nothing <: TypeVariable(T)`
4. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(T) <: kotlin/Comparable<kotlin/Nothing>`
5. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/Nothing>?`
6. Choose `TypeVariable(T)` with `Readiness(
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
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
7. `TypeVariable(T) == kotlin/String` _from Fix variable T_
8. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/String`
    2. `TypeVariable(T) <: kotlin/Comparable<kotlin/String>`