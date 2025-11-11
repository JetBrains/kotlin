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

#### Candidate 1: `FirNamedFunctionSymbol /ifTrue` --- `fun <T : Any> ifTrue(condition: Boolean, exec: () -> T?): T?↩`
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

#### Candidate 1: `FirNamedFunctionSymbol /decode` --- `fun decode(src: String): String↩`
##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument R|<local>/token|_

### Call 4

```
parse#(R|/decode|(R|<local>/token|))
```

#### Candidate 1: `FirNamedFunctionSymbol /parse` --- `fun <reified P : Any> parse(text: String): P↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(P)` for `FirNamedFunctionSymbol /parse`s parameter 0
2. `TypeVariable(P) <: kotlin/Any` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument R|/decode|(R|<local>/token|)_

##### Call Completion:

1. Choose `TypeVariable(P)` with `FORBIDDEN`

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

1. Choose `TypeVariable(K)` with `FORBIDDEN`
    1. `TypeVariable(P)` is `FORBIDDEN`

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

#### Candidate 1: `FirNamedFunctionSymbol /ifTrue` --- `fun <T : Any> ifTrue(condition: Boolean, exec: () -> T?): T?↩`
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
8. Choose `TypeVariable(P)` with `READY_FOR_FIXATION_REIFIED`
    1. `TypeVariable(T)` is `READY_FOR_FIXATION_UPPER`
    2. `TypeVariable(K)` is `READY_FOR_FIXATION_UPPER`
9. `TypeVariable(P) == Result` _from Fix variable P_
10. Combine `TypeVariable(P) == Result` with `TypeVariable(P) <: TypeVariable(K)`
    1. `Result <: TypeVariable(K)`
11. Combine `TypeVariable(P) == Result` with `TypeVariable(P) <: TypeVariable(T)?`
    1. `Result <: TypeVariable(T)`
12. Choose `TypeVariable(T)` with `READY_FOR_FIXATION_LOWER`
    1. `TypeVariable(K)` is `READY_FOR_FIXATION_LOWER`
13. `TypeVariable(T) == Result` _from Fix variable T_
14. Choose `TypeVariable(K)` with `READY_FOR_FIXATION_LOWER`
15. `TypeVariable(K) == Result?` _from Fix variable K_