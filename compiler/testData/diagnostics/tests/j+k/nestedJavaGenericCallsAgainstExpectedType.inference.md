## `Source session for module <main>`

### Call 1

```
Box#
```

#### Candidate 1: `FirRegularClassSymbol Box` --- `class Box<A : Any!> : Any`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(A)` for `FirRegularClassSymbol Box`s parameter 0

### Call 2

```
Box#
```

#### Candidate 1: `FirRegularClassSymbol Box` --- `class Box<A : Any!> : Any`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(A)` for `FirRegularClassSymbol Box`s parameter 0

### Call 3

```
Q|Box|.create2#(R|<local>/s|)
```

#### Candidate 1: `FirNamedFunctionSymbol /Box.create2` --- `static fun <A2 : Any!> create2(value: A2!): Box<A2!>!`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(A2)` for `FirNamedFunctionSymbol /Box.create2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String? <: TypeVariable(A2)!` _from Argument R|<local>/s|_
    1. `kotlin/String? <: TypeVariable(A2)`

##### Call Completion:

1. Choose `TypeVariable(A2)` with `Readiness(
   	false ALLOWED
   	false HAS_PROPER_CONSTRAINTS
   	false HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	false HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	false HAS_PROPER_NON_ILT_CONSTRAINT
   	false HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`

### Call 4

```
Q|Box|.create1#(Q|Box|.R?C|/Box.create2|(R|<local>/s|))
```

#### Candidate 1: `FirNamedFunctionSymbol /Box.create1` --- `static fun <A1 : Any!> create1(value: A1!): Box<A1!>!`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(A1)` for `FirNamedFunctionSymbol /Box.create1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `Box<TypeVariable(A2)!>! <: TypeVariable(A1)!` _from Argument Q|Box|.R?C|/Box.create2|(R|<local>/s|)_
    1. `Box<TypeVariable(A2)!>! <: TypeVariable(A1)`

##### Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `Box<TypeVariable(A1)!>! <: Box<Box<kotlin/String>>` _from ExpectedType for some call_
    1. `TypeVariable(A1) <: Box<kotlin/String>!`
    2. `Box<kotlin/String>! <: TypeVariable(A1)`
2. Combine `Box<TypeVariable(A2)!>! <: TypeVariable(A1)` with `TypeVariable(A1) <: Box<kotlin/String>!`
    1. `TypeVariable(A2) <: kotlin/String!`
    2. `kotlin/String! <: TypeVariable(A2)`
3. Combine `TypeVariable(A1) <: Box<kotlin/String>!` with `Box<kotlin/String>! <: TypeVariable(A1)`
    1. `TypeVariable(A1) == Box<kotlin/String>!`
4. Combine `TypeVariable(A2) <: kotlin/String!` with `kotlin/String! <: TypeVariable(A2)`
    1. `TypeVariable(A2) == kotlin/String!`

##### Call Completion:

1. Choose `TypeVariable(A2)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(A1)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	 true HAS_PROPER_EQUALITY_CONSTRAINT
       	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(A2) == kotlin/String!` _from Fix variable A2_
3. Combine `TypeVariable(A2) == kotlin/String!` with `Box<TypeVariable(A2)!>! <: TypeVariable(A1)`
    1. `Box<kotlin/String!>! <: TypeVariable(A1)`
4. Choose `TypeVariable(A1)` with `Readiness(
   	 true ALLOWED
   	 true HAS_PROPER_CONSTRAINTS
   	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
   	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
   	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
   	 true HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
   	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
   	false REIFIED
   	 true HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
   	 true HAS_PROPER_NON_ILT_CONSTRAINT
   	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
   	 true HAS_PROPER_EQUALITY_CONSTRAINT
   	 true HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
5. `TypeVariable(A1) == Box<kotlin/String>!` _from Fix variable A1_
