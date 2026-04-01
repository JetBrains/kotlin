## `Source session for module <main>`

### Call 1

```
R|<local>/x|.toArray#(R|<local>/y|)
```

#### Candidate 1: `FirNamedFunctionSymbol java/util/ArrayList.toArray` --- `fun <T : Any!> toArray(p0: ft<Array<T!>, Array<out T!>?>): ft<Array<T!>, Array<out T!>?>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T)` for `FirNamedFunctionSymbol java/util/ArrayList.toArray`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/Array<kotlin/String?> <: ft<kotlin/Array<TypeVariable(T)!>, kotlin/Array<out TypeVariable(T)!>?>` _from Argument R|<local>/y|_
    1. `kotlin/String? <: TypeVariable(T)`

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
Q|J|.asList#(R|<local>/x|.R?C|java/util/ArrayList.toArray|(R|<local>/y|))
```

#### Candidate 1: `FirNamedFunctionSymbol /J.asList` --- `static fun <F : Any!> asList(a: ft<Array<F!>, Array<out F!>?>): ft<MutableList<F!>, List<F!>?>↩`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(F)` for `FirNamedFunctionSymbol /J.asList`s parameter 0

##### Resolution Stages > CheckArguments:

1. `ft<kotlin/Array<TypeVariable(T)!>, kotlin/Array<out TypeVariable(T)!>?> <: ft<kotlin/Array<TypeVariable(F)!>, kotlin/Array<out TypeVariable(F)!>?>` _from Argument R|<local>/x|.R?C|java/util/ArrayList.toArray|(R|<local>/y|)_
    1. `ft<TypeVariable(T) & Any, TypeVariable(T)?> <: TypeVariable(F)`
    2. `TypeVariable(T) <: TypeVariable(F)!`
2. Combine `kotlin/String? <: TypeVariable(T)` with `TypeVariable(T) <: TypeVariable(F)!`
    1. `kotlin/String? <: TypeVariable(F)`

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `ft<kotlin/collections/MutableList<TypeVariable(F)!>, kotlin/collections/List<TypeVariable(F)!>?> <: kotlin/collections/List<kotlin/String>` _from ExpectedType for some call_
    1. `TypeVariable(F) <: kotlin/String!`
2. Combine `ft<TypeVariable(T) & Any, TypeVariable(T)?> <: TypeVariable(F)` with `TypeVariable(F) <: kotlin/String!`
    1. `TypeVariable(T) <: kotlin/String?`
3. Combine `TypeVariable(F) <: kotlin/String!` with `TypeVariable(T) <: TypeVariable(F)!`
    1. `TypeVariable(T) <: kotlin/String!`
4. Combine `kotlin/String? <: TypeVariable(T)` with `TypeVariable(T) <: kotlin/String?`
    1. `TypeVariable(T) == kotlin/String?`
5. Combine `TypeVariable(T) == kotlin/String?` with `TypeVariable(T) <: TypeVariable(F)!`
    1. `kotlin/String? <: TypeVariable(F)`
6. Combine `TypeVariable(T) == kotlin/String?` with `ft<TypeVariable(T) & Any, TypeVariable(T)?> <: TypeVariable(F)`
    1. `kotlin/String! <: TypeVariable(F)`
7. Combine `TypeVariable(F) <: kotlin/String!` with `kotlin/String! <: TypeVariable(F)`
    1. `TypeVariable(F) == kotlin/String!`

##### Call Completion:

1. Choose `TypeVariable(F)` with `Readiness(
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
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(F) == kotlin/String!` _from Fix variable F_
3. Combine `ft<TypeVariable(T) & Any, TypeVariable(T)?> <: TypeVariable(F)` with `TypeVariable(F) == kotlin/String!`
    1. `TypeVariable(T) <: kotlin/String?`
4. Combine `TypeVariable(F) == kotlin/String!` with `TypeVariable(T) <: TypeVariable(F)!`
    1. `TypeVariable(T) <: kotlin/String!`
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
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
6. `TypeVariable(T) == kotlin/String?` _from Fix variable T_