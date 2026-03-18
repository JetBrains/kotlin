## `Source session for module <main>`

### Call 1

```
JavaBox#(Null(null))
```

#### Candidate 1: `FirConstructorSymbol /JavaBox.JavaBox` --- `constructor<T : Any!>(b: T!): JavaBox<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol JavaBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(T)!` _from Argument Null(null)_
    1. `kotlin/Nothing? <: TypeVariable(T)`

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
R|<local>/k|.foo#(R?C|/JavaBox.JavaBox|(Null(null)))
```

#### Candidate 1: `FirNamedFunctionSymbol /K.foo` --- `fun foo(a: JavaBox<out String>): Unit↩`
##### Resolution Stages > CheckArguments:

1. `JavaBox<TypeVariable(T)> <: JavaBox<out kotlin/String>` _from Argument R?C|/JavaBox.JavaBox|(Null(null))_
    1. `TypeVariable(T) <: kotlin/String`

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
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == kotlin/Nothing?` _from Fix variable T_

### Call 3

```
JavaBox#(Null(null))
```

#### Candidate 1: `FirConstructorSymbol /JavaBox.JavaBox` --- `constructor<T : Any!>(b: T!): JavaBox<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol JavaBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(T)!` _from Argument Null(null)_
    1. `kotlin/Nothing? <: TypeVariable(T)`

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
foo2#<R|kotlin/String|>(R?C|/JavaBox.JavaBox|(Null(null)))
```

#### Candidate 1: `FirNamedFunctionSymbol /foo2` --- `fun <S> foo2(a: JavaBox<out S>): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(S)` for `FirNamedFunctionSymbol /foo2`s parameter 0
2. `TypeVariable(S) == kotlin/String` _from TypeParameter R|kotlin/String|_

##### Resolution Stages > CheckArguments:

1. `JavaBox<TypeVariable(T)> <: JavaBox<out TypeVariable(S)>` _from Argument R?C|/JavaBox.JavaBox|(Null(null))_
    1. `TypeVariable(T) <: TypeVariable(S)`
2. Combine `TypeVariable(T) <: TypeVariable(S)` with `TypeVariable(S) == kotlin/String`
    1. `TypeVariable(T) <: kotlin/String`
3. Combine `kotlin/Nothing? <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(S)`
    1. `kotlin/Nothing? <: TypeVariable(S)`
4. __NewConstraintError: `kotlin/Nothing? <: kotlin/String`__

##### Call Completion:

1. Choose `TypeVariable(S)` with `Readiness(
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
    1. `TypeVariable(T)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(S) == kotlin/String` _from Fix variable S_
3. Choose `TypeVariable(T)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
4. `TypeVariable(T) == kotlin/Nothing?` _from Fix variable T_

### Call 5

```
JavaBox#(Null(null))
```

#### Candidate 1: `FirConstructorSymbol /JavaBox.JavaBox` --- `constructor<T : Any!>(b: T!): JavaBox<T>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol JavaBox`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Nothing? <: TypeVariable(T)!` _from Argument Null(null)_
    1. `kotlin/Nothing? <: TypeVariable(T)`

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

### Call 6

```
foo3#(R?C|/JavaBox.JavaBox|(Null(null)))
```

#### Candidate 1: `FirNamedFunctionSymbol /foo3` --- `fun foo3(a: JavaBox<out String>): Unit`
##### Resolution Stages > CheckArguments:

1. `JavaBox<TypeVariable(T)> <: JavaBox<out kotlin/String>` _from Argument R?C|/JavaBox.JavaBox|(Null(null))_
    1. `TypeVariable(T) <: kotlin/String`

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
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(T) == kotlin/Nothing?` _from Fix variable T_