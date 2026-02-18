## `Source session for module <main>`

### Call 1

```
expectThroughTV#(R|<local>/x|, <collectionLiteralCall>(IntegerLiteral(42)))
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /expectThroughTV`s parameter 0

##### Resolution Stages > CheckArguments:

1. `it(A & B) <: TypeVariable(T)` _from Argument R|<local>/x|_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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
<collectionLiteralCall>(IntegerLiteral(42))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL` --- `fun <K> DANGLING_COLLECTION_LITERAL_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 42 <: TypeVariable(K)` _from Argument IntegerLiteral(42)_

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Int` _from Fix variable K_

### Call 1

```
expectThroughTV#(R|<local>/x|, <collectionLiteralCall>(IntegerLiteral(42)))
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Continue Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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
2. `TypeVariable(T) == it(A & B)` _from Fix variable T_

### Call 3

```
expectThroughTV#(R|<local>/x|, <collectionLiteralCall>())
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /expectThroughTV`s parameter 0

##### Resolution Stages > CheckArguments:

1. `it(A & B) <: TypeVariable(T)` _from Argument R|<local>/x|_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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

### Call 4

```
<collectionLiteralCall>()
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL` --- `fun <K> DANGLING_COLLECTION_LITERAL_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL`s parameter 0

##### Call Completion:

1. Choose `TypeVariable(K)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(K)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
3. __NotEnoughInformationForTypeParameter__
4. `TypeVariable(K) == ERROR CLASS: Cannot infer argument for type parameter K` _from Fix variable K_

### Call 3

```
expectThroughTV#(R|<local>/x|, <collectionLiteralCall>())
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Continue Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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
2. `TypeVariable(T) == it(A & B)` _from Fix variable T_

### Call 5

```
when () {
    Boolean(true) ->  {
        object : R|A|, R|B| {
            private constructor(): R|<anonymous>| {
                super<R|kotlin/Any|>()
            }

        }

    }
    else ->  {
        object : R|B|, R|A| {
            private constructor(): R|<anonymous>| {
                super<R|kotlin/Any|>()
            }

        }

    }
}

```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL` --- `fun <K> WHEN_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/WHEN_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `<anonymous> <: TypeVariable(K)` _from Argument object : R|A|, R|B| {↩    private constructor(): R|<anonymous>| {↩        super<R|kotlin/Any|>()↩    }↩↩}↩_

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == it(A & B)` _from Fix variable K_

### Call 6

```
expectThroughTV#(when () {
    Boolean(true) ->  {
        object : R|A|, R|B| {
            private constructor(): R|<anonymous>| {
                super<R|kotlin/Any|>()
            }

        }

    }
    else ->  {
        object : R|B|, R|A| {
            private constructor(): R|<anonymous>| {
                super<R|kotlin/Any|>()
            }

        }

    }
}
, <collectionLiteralCall>(String(42)))
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /expectThroughTV`s parameter 0

##### Resolution Stages > CheckArguments:

1. `it(A & B) <: TypeVariable(T)` _from Argument when () {↩    Boolean(true) ->  {↩        object : R|A|, R|B| {↩            private constructor(): R|<anonymous>| {↩                super<R|kotlin/Any|>()↩            }↩↩        }↩↩    }↩    else ->  {↩        object : R|B|, R|A| {↩            private constructor(): R|<anonymous>| {↩                super<R|kotlin/Any|>()↩            }↩↩        }↩↩    }↩}↩_

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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

### Call 7

```
<collectionLiteralCall>(String(42))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL` --- `fun <K> DANGLING_COLLECTION_LITERAL_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(K)` _from Argument String(42)_

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/String` _from Fix variable K_

### Call 6

```
expectThroughTV#(when () {
    Boolean(true) ->  {
        object : R|A|, R|B| {
            private constructor(): R|<anonymous>| {
                super<R|kotlin/Any|>()
            }

        }

    }
    else ->  {
        object : R|B|, R|A| {
            private constructor(): R|<anonymous>| {
                super<R|kotlin/Any|>()
            }

        }

    }
}
, <collectionLiteralCall>(String(42)))
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Continue Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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
2. `TypeVariable(T) == it(A & B)` _from Fix variable T_

### Call 8

```
Null(null)!!
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/CHECK_NOT_NULL_CALL` --- `fun <K> CHECK_NOT_NULL_CALL(arg: K?): K & Any↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/CHECK_NOT_NULL_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(K)?` _from Argument Null(null)_
    1. `kotlin/Nothing <: TypeVariable(K)`

##### Resolution Stages > CheckIncompatibleTypeVariableUpperBounds:

1. `TypeVariable(K) & Any <: U` _from ExpectedType for some call_
    1. `TypeVariable(K) <: U?`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Nothing` _from Fix variable K_

### Call 9

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    lval x: <implicit> = get#()
    (x# as B)
    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)
    put#(A#.of#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/buildBox` --- `fun <X> buildBox(block: Box<X>.() -> Unit): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(X)` for `FirNamedFunctionSymbol <local>/buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `<local>/Box<TypeVariable(X)>.() -> kotlin/Unit <: <local>/Box<TypeVariable(X)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    lval x: <implicit> = get#()↩    (x# as B)↩    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)↩    put#(A#.of#())↩}↩_

##### Call Completion:

1. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 10

```
expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), R?C|<local>/x|)
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /expectThroughTV`s parameter 0

##### Resolution Stages > CheckArguments:

1. `it(B & TypeVariable(X) & Any) <: TypeVariable(T)` _from Argument R?C|<local>/x|_
    1. `TypeVariable(X) <: TypeVariable(T)?`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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

### Call 11

```
put#(Q|A|.R|/A.Companion.of|())
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/Box.put` --- `fun put(x: A): Unit↩`
##### Resolution Stages > CheckArguments:

1. `A <: TypeVariable(X)` _from Argument Q|A|.R|/A.Companion.of|()_
2. Combine `A <: TypeVariable(X)` with `TypeVariable(X) <: TypeVariable(T)?`
    1. `A <: TypeVariable(T)`

### Call 9

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    lval x: <implicit> = get#()
    (x# as B)
    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)
    put#(A#.of#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/buildBox` --- `fun <X> buildBox(block: Box<X>.() -> Unit): Unit`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(X)` with `Readiness(
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
    1. `TypeVariable(T)` is `Readiness(
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

### Call 12

```
Q|A.Companion|.of#(IntegerLiteral(42))
```

#### Candidate 1: `FirNamedFunctionSymbol /A.Companion.of` --- `fun of(vararg x: Int): A`
##### Resolution Stages > CheckArguments:

1. `ILT: 42 <: kotlin/Int` _from Argument IntegerLiteral(42)_

### Call 9

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    lval x: <implicit> = get#()
    (x# as B)
    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)
    put#(A#.of#())
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/buildBox` --- `fun <X> buildBox(block: Box<X>.() -> Unit): Unit`
##### Continue Continue Call Completion:

1. `A <: TypeVariable(T)` _from Argument Q|A.Companion|.R?C|/A.Companion.of|(IntegerLiteral(42))_
2. Choose `TypeVariable(X)` with `Readiness(
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
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. `TypeVariable(X) == A` _from Fix variable X_
4. Combine `TypeVariable(X) == A` with `it(B & TypeVariable(X) & Any) <: TypeVariable(T)`
    1. `it(B & A) <: TypeVariable(T)`
5. Choose `TypeVariable(T)` with `Readiness(
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
6. `TypeVariable(T) == A` _from Fix variable T_

### Call 13

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    lval x: <implicit> = get#()
    (x# as B)
    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)
    Unit#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/buildBox` --- `fun <X> buildBox(block: Box<X>.() -> Unit): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(X)` for `FirNamedFunctionSymbol <local>/buildBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `<local>/Box<TypeVariable(X)>.() -> kotlin/Unit <: <local>/Box<TypeVariable(X)>.() -> kotlin/Unit` _from Argument <L> = buildBox <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {↩    lval x: <implicit> = get#()↩    (x# as B)↩    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)↩    Unit#↩}↩_

##### Call Completion:

1. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 14

```
expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), R?C|<local>/x|)
```

#### Candidate 1: `FirNamedFunctionSymbol /expectThroughTV` --- `fun <T> expectThroughTV(x: T, y: T): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /expectThroughTV`s parameter 0

##### Resolution Stages > CheckArguments:

1. `it(B & TypeVariable(X) & Any) <: TypeVariable(T)` _from Argument R?C|<local>/x|_
    1. `TypeVariable(X) <: TypeVariable(T)?`

##### Call Completion:

1. Choose `TypeVariable(T)` with `Readiness(
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

### Call 13

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    lval x: <implicit> = get#()
    (x# as B)
    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)
    Unit#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/buildBox` --- `fun <X> buildBox(block: Box<X>.() -> Unit): Unit`
##### Continue Call Completion:

1. `kotlin/Unit <: kotlin/Unit` _from LambdaArgument_
2. Choose `TypeVariable(X)` with `Readiness(
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
    1. `TypeVariable(T)` is `Readiness(
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

### Call 15

```
<collectionLiteralCall>(IntegerLiteral(42))
```

#### Candidate 1: `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL` --- `fun <K> DANGLING_COLLECTION_LITERAL_CALL(vararg branches: K): K↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirSyntheticFunctionSymbol _synthetic/DANGLING_COLLECTION_LITERAL_CALL`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ILT: 42 <: TypeVariable(K)` _from Argument IntegerLiteral(42)_

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == kotlin/Int` _from Fix variable K_

### Call 13

```
buildBox#(<L> = buildBox@fun <implicit>.<anonymous>(): <implicit> <inline=Unknown>  {
    lval x: <implicit> = get#()
    (x# as B)
    expectThroughTV#(<collectionLiteralCall>(IntegerLiteral(42)), x#)
    Unit#
}
)
```

#### Candidate 1: `FirNamedFunctionSymbol <local>/buildBox` --- `fun <X> buildBox(block: Box<X>.() -> Unit): Unit`
##### Continue Continue Call Completion:

1. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
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
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. Choose `TypeVariable(X)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
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
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	false HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
3. __NotEnoughInformationForTypeParameter__
4. `TypeVariable(X) == ERROR CLASS: Cannot infer argument for type parameter X` _from Fix variable X_
5. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. __NotEnoughInformationForTypeParameter__
7. `TypeVariable(T) == ERROR CLASS: Cannot infer argument for type parameter T` _from Fix variable T_