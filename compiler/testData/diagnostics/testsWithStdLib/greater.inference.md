## `Source session for module <main>`

### Call 1

```
Expression#(R|<local>/other|)
```

#### Candidate 1: `FirConstructorSymbol /Expression.Expression` --- `constructor<T>(x: T): Expression<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol Expression`s parameter 0

##### Resolution Stages > CheckArguments:

1. `T <: TypeVariable(T)` _from Argument R|<local>/other|_

### Call 2

```
GreaterOp#(this@R|/greater|, R?C|/Expression.Expression|(R|<local>/other|))
```

#### Candidate 1: `FirConstructorSymbol /GreaterOp.GreaterOp` --- `constructor(expr1: Expression<*>, expr2: Expression<*>): GreaterOp`
##### Resolution Stages > CheckArguments:

1. `Expression<CapturedType(in S)> <: Expression<*>` _from Argument this|/greater|_
2. `Expression<TypeVariable(T)> <: Expression<*>` _from Argument R?C|/Expression.Expression|(R|<local>/other|)_

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `GreaterOp <: GreaterOp` _from ExpectedType for some call_

##### Call Completion:

1. `TypeVariable(T) == T` _from Fix variable T_

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

##### Readiness of Variables:

1. ConeTypeVariableTypeConstructor(T) is WITH_COMPLEX_DEPENDENCY_AND_NO_CONCRETE_CONSTRAINTS
2. ConeTypeVariableTypeConstructor(S) is WITH_COMPLEX_DEPENDENCY

##### Call Completion:

1. `TypeVariable(S) == kotlin/Long` _from Fix variable S_
2. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: TypeVariable(T)?`
    1. `kotlin/Long <: TypeVariable(T)`
3. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(T) <: kotlin/Long`
4. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Long?`
5. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(S) & Any>`
    1. `TypeVariable(T) <: kotlin/Comparable<kotlin/Long>`
6. Combine `TypeVariable(S) == kotlin/Long` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/Long>?`
7. Combine `kotlin/Long <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/Long`
    1. `TypeVariable(T) == kotlin/Long`

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

##### Readiness of Variables:

1. ConeTypeVariableTypeConstructor(T) is WITH_COMPLEX_DEPENDENCY_BUT_PROPER_EQUALITY_CONSTRAINT
2. ConeTypeVariableTypeConstructor(S) is WITH_COMPLEX_DEPENDENCY

##### Call Completion:

1. `TypeVariable(T) == kotlin/String` _from Fix variable T_
2. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) == kotlin/String`
    1. `TypeVariable(S) <: kotlin/String?`
3. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/String`
    2. `TypeVariable(T) <: kotlin/Comparable<kotlin/String>`
4. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/String>?`
5. __InferredEmptyIntersectionWarning__
6. `TypeVariable(S) == it(kotlin/Long & kotlin/String)` _from Fix variable S_
7. Combine `TypeVariable(S) == it(kotlin/Long & kotlin/String)` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
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

##### Readiness of Variables:

1. ConeTypeVariableTypeConstructor(T) is WITH_COMPLEX_DEPENDENCY_BUT_PROPER_EQUALITY_CONSTRAINT
2. ConeTypeVariableTypeConstructor(S) is WITH_COMPLEX_DEPENDENCY_BUT_PROPER_EQUALITY_CONSTRAINT

##### Call Completion:

1. `TypeVariable(T) == kotlin/String` _from Fix variable T_
2. Combine `TypeVariable(S) & Any <: TypeVariable(T)` with `TypeVariable(T) == kotlin/String`
    1. `TypeVariable(S) <: kotlin/String?`
3. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(T) <: kotlin/Comparable<TypeVariable(T)>`
    1. `TypeVariable(T) <: kotlin/String`
    2. `TypeVariable(T) <: kotlin/Comparable<kotlin/String>`
4. Combine `TypeVariable(T) == kotlin/String` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(T)>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/String>?`
5. `TypeVariable(S) == kotlin/Nothing` _from Fix variable S_
6. Combine `TypeVariable(S) == kotlin/Nothing` with `TypeVariable(S) <: kotlin/Comparable<TypeVariable(S) & Any>?`
    1. `TypeVariable(S) <: kotlin/Comparable<kotlin/Nothing>?`