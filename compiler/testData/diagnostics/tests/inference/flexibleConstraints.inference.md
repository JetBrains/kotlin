## `Source session for module <main>`

### Call 1

```
Q|JavaClass|.consume#(String())
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.consume` --- `static fun <C : Any!> consume(c: C!): Unit↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(C)` for `FirNamedFunctionSymbol /JavaClass.consume`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: TypeVariable(C)!` _from Argument String()_
    1. `kotlin/String! <: TypeVariable(C)`

##### Call Completion:

1. Choose `TypeVariable(C)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	 true HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(C) == kotlin/String!` _from Fix variable C_

### Call 2

```
Q|JavaClass|.consume#(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.consume` --- `static fun <C : Any!> consume(c: C!): Unit↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(C)` for `FirNamedFunctionSymbol /JavaClass.consume`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(C)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(C)`

##### Call Completion:

1. Choose `TypeVariable(C)` with `Readiness(
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
2. `TypeVariable(C) == kotlin/String?` _from Fix variable C_

### Call 3

```
Q|JavaClass|.transform#(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.transform` --- `static fun <T : Any!> transform(t: T!): T!↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /JavaClass.transform`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(T)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(T)`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == kotlin/String?` _from Fix variable T_

### Call 4

```
eatString#(Q|JavaClass|.R|/JavaClass.transform|<R|kotlin/String?|>(R|<local>/s|))
```

#### Candidate 1: `FirNamedFunctionSymbol /eatString` --- `fun eatString(s: String): Unit`
##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: kotlin/String` _from Argument Q|JavaClass|.R|/JavaClass.transform|<R|kotlin/String?|>(R|<local>/s|)_
2. __NewConstraintError: `kotlin/String? <: kotlin/String`__

### Call 5

```
Q|JavaClass|.transform#(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.transform` --- `static fun <T : Any!> transform(t: T!): T!↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /JavaClass.transform`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(T)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(T)`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == kotlin/String?` _from Fix variable T_

### Call 6

```
eatString#(R|<local>/res|)
```

#### Candidate 1: `FirNamedFunctionSymbol /eatString` --- `fun eatString(s: String): Unit`
##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: kotlin/String` _from Argument R|<local>/res|_
2. __NewConstraintError: `kotlin/String? <: kotlin/String`__

### Call 7

```
Q|JavaClass|.transformNotNull#(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.transformNotNull` --- `static fun <T : Any!> transformNotNull(t: T!): @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  T & Any↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /JavaClass.transformNotNull`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(T)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(T)`

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `@EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  TypeVariable(T) & Any <: kotlin/String` _from ExpectedType for some call_
    1. `TypeVariable(T) <: kotlin/String?`
2. Combine `kotlin/String? <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/String?`
    1. `TypeVariable(T) == kotlin/String?`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == kotlin/String?` _from Fix variable T_

### Call 8

```
Q|JavaClass|.transformNotNull#(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.transformNotNull` --- `static fun <T : Any!> transformNotNull(t: T!): @EnhancedNullability @R|org/jetbrains/annotations/NotNull|()  T & Any↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /JavaClass.transformNotNull`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(T)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(T)`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == kotlin/String?` _from Fix variable T_

### Call 9

```
Q|JavaClass|.transform#(R|<local>/arg|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.transform` --- `static fun <T : Any!> transform(t: T!): T!↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /JavaClass.transform`s parameter 0

##### Resolution Stages > CheckArguments:

1. `R? <: TypeVariable(T)!` _from Argument R|<local>/arg|_
    1. `R? <: TypeVariable(T)`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == R?` _from Fix variable T_

### Call 10

```
Q|JavaClass|.consumeWithBounds#<R|kotlin/String|, >(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /JavaClass.consumeWithBounds` --- `static fun <T : Any!, U : T!> consumeWithBounds(u: U!): Unit↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol /JavaClass.consumeWithBounds`s parameter 0
2. New `TypeVariable(U)` for `FirNamedFunctionSymbol /JavaClass.consumeWithBounds`s parameter 1
3. `TypeVariable(U) <: TypeVariable(T)!` _from DeclaredUpperBound_
    1. `TypeVariable(U)! <: TypeVariable(T)`
4. `TypeVariable(T) == kotlin/String!` _from TypeParameter R|kotlin/String|_
5. Combine `TypeVariable(U)! <: TypeVariable(T)` with `TypeVariable(T) == kotlin/String!`
    1. `TypeVariable(U) <: kotlin/String!`

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(U)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(U)`
2. Combine `kotlin/String? <: TypeVariable(U)` with `TypeVariable(U) <: TypeVariable(T)!`
    1. `kotlin/String? <: TypeVariable(T)`

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
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(U)` is `Readiness(
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
2. `TypeVariable(T) == kotlin/String!` _from Fix variable T_
3. Combine `TypeVariable(U)! <: TypeVariable(T)` with `TypeVariable(T) == kotlin/String!`
    1. `TypeVariable(U) <: kotlin/String!`
4. Choose `TypeVariable(U)` with `Readiness(
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
5. `TypeVariable(U) == kotlin/String?` _from Fix variable U_