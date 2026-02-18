## `Source session for module <main>`

### Call 1

```
super<R|Expression<A>|>()
```

#### Candidate 1: `FirConstructorSymbol /Expression.Expression` --- `constructor<K>(): Expression<K>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirRegularClassSymbol Expression`s parameter 0
2. `TypeVariable(K) == A` _from TypeParameter R|A|_

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
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
2. `TypeVariable(K) == A` _from Fix variable K_

### Call 2

```
wrap#(R|<local>/t|)
```

#### Candidate 1: `FirNamedFunctionSymbol /wrap` --- `fun <K, R : K?> Expression<in R>.wrap(value: K): QueryParameter<K>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(K)` for `FirNamedFunctionSymbol /wrap`s parameter 0
2. New `TypeVariable(R)` for `FirNamedFunctionSymbol /wrap`s parameter 1
3. `TypeVariable(R) <: TypeVariable(K)?` _from DeclaredUpperBound_
    1. `TypeVariable(R) & Any <: TypeVariable(K)`

##### Resolution Stages > CheckExtensionReceiver:

1. `Expression<M> <: Expression<in TypeVariable(R)>` _from Receiver this|/rem|_
    1. `TypeVariable(R) <: M`

##### Resolution Stages > CheckArguments:

1. `Z <: TypeVariable(K)` _from Argument R|<local>/t|_

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
    1. `TypeVariable(R)` is `Readiness(
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

### Call 3

```
ModOp#(this@R|/rem|, R?C|/wrap|(R|<local>/t|))
```

#### Candidate 1: `FirConstructorSymbol /ModOp.ModOp` --- `constructor<T : Number?, S : Number?>(expr1: Expression<T>, expr2: Expression<S>): ModOp<T, S>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirRegularClassSymbol ModOp`s parameter 0
2. New `TypeVariable(S)` for `FirRegularClassSymbol ModOp`s parameter 1
3. `TypeVariable(T) <: kotlin/Number?` _from DeclaredUpperBound_
4. `TypeVariable(S) <: kotlin/Number?` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `Expression<M> <: Expression<TypeVariable(T)>` _from Argument this|/rem|_
    1. `M <: TypeVariable(T)`
    2. `TypeVariable(T) <: M`
2. Combine `M <: TypeVariable(T)` with `TypeVariable(T) <: M`
    1. `TypeVariable(T) == M`
3. `QueryParameter<TypeVariable(K)> <: Expression<TypeVariable(S)>` _from Argument R?C|/wrap|(R|<local>/t|)_
    1. `TypeVariable(K) <: TypeVariable(S)`
    2. `TypeVariable(S) <: TypeVariable(K)`
4. Combine `TypeVariable(K) <: TypeVariable(S)` with `TypeVariable(S) <: kotlin/Number?`
    1. `TypeVariable(K) <: kotlin/Number?`
5. Combine `TypeVariable(R) & Any <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(S)`
    1. `TypeVariable(R) & Any <: TypeVariable(S)`
    2. `TypeVariable(R) <: TypeVariable(S)?`
6. Combine `Z <: TypeVariable(K)` with `TypeVariable(K) <: TypeVariable(S)`
    1. `Z <: TypeVariable(S)`
7. Combine `TypeVariable(K) <: TypeVariable(S)` with `TypeVariable(S) <: TypeVariable(K)`
    1. `TypeVariable(K) == TypeVariable(S)`
8. Combine `TypeVariable(K) <: TypeVariable(S)` with `TypeVariable(S) <: TypeVariable(K)`
    1. `TypeVariable(S) == TypeVariable(K)`
9. Combine `TypeVariable(R) & Any <: TypeVariable(K)` with `TypeVariable(K) <: kotlin/Number?`
    1. `TypeVariable(R) <: kotlin/Number?`
10. Combine `TypeVariable(K) == TypeVariable(S)` with `TypeVariable(K) <: kotlin/Number?`
    1. `TypeVariable(S) <: kotlin/Number?`
11. Combine `TypeVariable(R) & Any <: TypeVariable(S)` with `TypeVariable(S) == TypeVariable(K)`
    1. `TypeVariable(R) & Any <: TypeVariable(K)`
    2. `TypeVariable(R) <: TypeVariable(K)?`
12. Combine `Z <: TypeVariable(S)` with `TypeVariable(S) == TypeVariable(K)`
    1. `Z <: TypeVariable(K)`

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `ModOp<TypeVariable(T), TypeVariable(S)> <: ModOp<M, Z>` _from ExpectedType for some call_
    1. `TypeVariable(T) <: M`
    2. `M <: TypeVariable(T)`
    3. `TypeVariable(S) <: Z`
    4. `Z <: TypeVariable(S)`
2. Combine `Z <: TypeVariable(S)` with `TypeVariable(S) <: Z`
    1. `TypeVariable(S) == Z`
3. Combine `TypeVariable(S) == TypeVariable(K)` with `TypeVariable(S) == Z`
    1. `TypeVariable(K) <: Z`
4. Combine `TypeVariable(R) & Any <: TypeVariable(S)` with `TypeVariable(S) == Z`
    1. `TypeVariable(R) <: Z?`
5. Combine `TypeVariable(S) == Z` with `TypeVariable(S) == TypeVariable(K)`
    1. `Z <: TypeVariable(K)`
6. Combine `Z <: TypeVariable(K)` with `TypeVariable(K) <: Z`
    1. `TypeVariable(K) == Z`

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
    1. `TypeVariable(S)` is `Readiness(
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
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    3. `TypeVariable(R)` is `Readiness(
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
2. `TypeVariable(T) == M` _from Fix variable T_
3. Choose `TypeVariable(S)` with `Readiness(
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
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(R)` is `Readiness(
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
4. `TypeVariable(S) == Z` _from Fix variable S_
5. Combine `TypeVariable(S) == TypeVariable(K)` with `TypeVariable(S) == Z`
    1. `TypeVariable(K) <: Z`
6. Combine `TypeVariable(R) & Any <: TypeVariable(S)` with `TypeVariable(S) == Z`
    1. `TypeVariable(R) <: Z?`
7. Combine `TypeVariable(S) == Z` with `TypeVariable(S) == TypeVariable(K)`
    1. `Z <: TypeVariable(K)`
8. Choose `TypeVariable(K)` with `Readiness(
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
    1. `TypeVariable(R)` is `Readiness(
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
9. `TypeVariable(K) == Z` _from Fix variable K_
10. Choose `TypeVariable(R)` with `Readiness(
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
11. `TypeVariable(R) == it(M & Z?)` _from Fix variable R_