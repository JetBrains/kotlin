## `Source session for module <main>`

### Call 1

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X1>` _from ExpectedType for some call_

### Call 2

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X2>` _from ExpectedType for some call_

### Call 3

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X3>` _from ExpectedType for some call_

### Call 4

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X4>` _from ExpectedType for some call_

### Call 5

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X5>` _from ExpectedType for some call_

### Call 6

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X6>` _from ExpectedType for some call_

### Call 7

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X7>` _from ExpectedType for some call_

### Call 8

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X8>` _from ExpectedType for some call_

### Call 9

```
TODO#()
```

#### Candidate 1: `FirNamedFunctionSymbol kotlin/TODO` --- `@InlineOnly() fun TODO(): Nothing`
##### Continue Resolution Stages > CheckLambdaAgainstTypeVariableContradiction:

1. `kotlin/Nothing <: Column<X9>` _from ExpectedType for some call_

### Call 10

```
OtherFactory#()
```

#### Candidate 1: `FirConstructorSymbol /OtherFactory.OtherFactory` --- `constructor<T1 : ExtendedEntity<T1>>(): OtherFactory<T1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T1)` for `FirRegularClassSymbol OtherFactory`s parameter 0
2. `TypeVariable(T1) <: ExtendedEntity<TypeVariable(T1)>` _from DeclaredUpperBound_

##### Call Completion:

1. Choose `TypeVariable(T1)` with `Readiness(
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

### Call 11

```
string1#(String(C_1))
```

#### Candidate 1: `FirNamedFunctionSymbol /string1` --- `fun <X1> string1(name: String): Column<X1>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(X1)` for `FirNamedFunctionSymbol /string1`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument String(C_1)_

##### Call Completion:

1. Choose `TypeVariable(X1)` with `Readiness(
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

### Call 12

```
string2#(String(C_2))
```

#### Candidate 1: `FirNamedFunctionSymbol /string2` --- `fun <X2> string2(name: String): Column<X2>`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(X2)` for `FirNamedFunctionSymbol /string2`s parameter 0

##### Resolution Stages > CheckArguments:

1. `kotlin/String <: kotlin/String` _from Argument String(C_2)_

##### Call Completion:

1. Choose `TypeVariable(X2)` with `Readiness(
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

### Call 13

```
foo#(R?C|/OtherFactory.OtherFactory|(), R?C|/string1|(String(C_1)), R?C|/string2|(String(C_2)))
```

#### Candidate 1: `FirNamedFunctionSymbol /foo` --- `fun <T2 : Any> foo(factory: Factory<T2>, x1: Column<T2>, x2: Column<T2>): Unit`
##### Resolution Stages > CreateFreshTypeVariableSubstitutorStage:

1. New `TypeVariable(T2)` for `FirNamedFunctionSymbol /foo`s parameter 0
2. `TypeVariable(T2) <: kotlin/Any` _from DeclaredUpperBound_

##### Resolution Stages > CheckArguments:

1. `OtherFactory<TypeVariable(T1)> <: Factory<TypeVariable(T2)>` _from Argument R?C|/OtherFactory.OtherFactory|()_
    1. `TypeVariable(T1) <: TypeVariable(T2)`
    2. `TypeVariable(T2) <: TypeVariable(T1)`
2. Combine `TypeVariable(T1) <: TypeVariable(T2)` with `TypeVariable(T2) <: kotlin/Any`
    1. `TypeVariable(T1) <: kotlin/Any`
3. Combine `TypeVariable(T1) <: TypeVariable(T2)` with `TypeVariable(T2) <: TypeVariable(T1)`
    1. `TypeVariable(T1) == TypeVariable(T2)`
4. Combine `TypeVariable(T1) == TypeVariable(T2)` with `TypeVariable(T1) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(T2) <: ExtendedEntity<TypeVariable(T1)>`
5. Combine `TypeVariable(T1) <: TypeVariable(T2)` with `TypeVariable(T2) <: TypeVariable(T1)`
    1. `TypeVariable(T2) == TypeVariable(T1)`
6. Combine `TypeVariable(T1) == TypeVariable(T2)` with `TypeVariable(T1) <: kotlin/Any`
    1. `TypeVariable(T2) <: kotlin/Any`
7. Combine `TypeVariable(T2) == TypeVariable(T1)` with `TypeVariable(T2) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(T1) <: ExtendedEntity<TypeVariable(T1)>`
8. `Column<TypeVariable(X1)> <: Column<TypeVariable(T2)>` _from Argument R?C|/string1|(String(C_1))_
    1. `TypeVariable(X1) <: TypeVariable(T2)`
    2. `TypeVariable(T2) <: TypeVariable(X1)`
9. Combine `TypeVariable(X1) <: TypeVariable(T2)` with `TypeVariable(T2) <: kotlin/Any`
    1. `TypeVariable(X1) <: kotlin/Any`
10. Combine `TypeVariable(X1) <: TypeVariable(T2)` with `TypeVariable(T2) == TypeVariable(T1)`
    1. `TypeVariable(X1) <: TypeVariable(T1)`
11. Combine `TypeVariable(X1) <: TypeVariable(T2)` with `TypeVariable(T2) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(X1) <: ExtendedEntity<TypeVariable(T1)>`
12. Combine `TypeVariable(X1) <: TypeVariable(T2)` with `TypeVariable(T2) <: TypeVariable(X1)`
    1. `TypeVariable(X1) == TypeVariable(T2)`
13. Combine `TypeVariable(X1) <: TypeVariable(T2)` with `TypeVariable(T2) <: TypeVariable(X1)`
    1. `TypeVariable(T2) == TypeVariable(X1)`
14. Combine `TypeVariable(T2) == TypeVariable(T1)` with `TypeVariable(T2) == TypeVariable(X1)`
    1. `TypeVariable(T1) <: TypeVariable(X1)`
15. Combine `TypeVariable(X1) <: TypeVariable(T1)` with `TypeVariable(T1) <: TypeVariable(X1)`
    1. `TypeVariable(X1) == TypeVariable(T1)`
16. Combine `TypeVariable(X1) <: TypeVariable(T1)` with `TypeVariable(T1) <: TypeVariable(X1)`
    1. `TypeVariable(T1) == TypeVariable(X1)`
17. `Column<TypeVariable(X2)> <: Column<TypeVariable(T2)>` _from Argument R?C|/string2|(String(C_2))_
    1. `TypeVariable(X2) <: TypeVariable(T2)`
    2. `TypeVariable(T2) <: TypeVariable(X2)`
18. Combine `TypeVariable(X2) <: TypeVariable(T2)` with `TypeVariable(T2) <: kotlin/Any`
    1. `TypeVariable(X2) <: kotlin/Any`
19. Combine `TypeVariable(X2) <: TypeVariable(T2)` with `TypeVariable(T2) == TypeVariable(T1)`
    1. `TypeVariable(X2) <: TypeVariable(T1)`
20. Combine `TypeVariable(X2) <: TypeVariable(T2)` with `TypeVariable(T2) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(X2) <: ExtendedEntity<TypeVariable(T1)>`
21. Combine `TypeVariable(X2) <: TypeVariable(T2)` with `TypeVariable(T2) == TypeVariable(X1)`
    1. `TypeVariable(X2) <: TypeVariable(X1)`
22. Combine `TypeVariable(X2) <: TypeVariable(T2)` with `TypeVariable(T2) <: TypeVariable(X2)`
    1. `TypeVariable(X2) == TypeVariable(T2)`
23. Combine `TypeVariable(X2) <: TypeVariable(T2)` with `TypeVariable(T2) <: TypeVariable(X2)`
    1. `TypeVariable(T2) == TypeVariable(X2)`
24. Combine `TypeVariable(T2) == TypeVariable(T1)` with `TypeVariable(T2) == TypeVariable(X2)`
    1. `TypeVariable(T1) <: TypeVariable(X2)`
25. Combine `TypeVariable(T2) == TypeVariable(X1)` with `TypeVariable(T2) == TypeVariable(X2)`
    1. `TypeVariable(X1) <: TypeVariable(X2)`
26. Combine `TypeVariable(X2) <: TypeVariable(T1)` with `TypeVariable(T1) <: TypeVariable(X2)`
    1. `TypeVariable(X2) == TypeVariable(T1)`
27. Combine `TypeVariable(X2) <: TypeVariable(T1)` with `TypeVariable(T1) <: TypeVariable(X2)`
    1. `TypeVariable(T1) == TypeVariable(X2)`
28. Combine `TypeVariable(X2) <: TypeVariable(X1)` with `TypeVariable(X1) <: TypeVariable(X2)`
    1. `TypeVariable(X2) == TypeVariable(X1)`
29. Combine `TypeVariable(X2) <: TypeVariable(X1)` with `TypeVariable(X1) <: TypeVariable(X2)`
    1. `TypeVariable(X1) == TypeVariable(X2)`

##### Call Completion:

1. Choose `TypeVariable(T2)` with `Readiness(
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
   	false HAS_PROPER_EQUALITY_CONSTRAINT
   	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
   )`
    1. `TypeVariable(T1)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    2. `TypeVariable(X1)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
    3. `TypeVariable(X2)` is `Readiness(
       	 true ALLOWED
       	 true HAS_PROPER_CONSTRAINTS
       	 true HAS_NO_OUTER_TYPE_VARIABLE_DEPENDENCY
       	false HAS_CAPTURED_UPPER_BOUND_WITH_SELF_TYPES
       	 true HAS_PROPER_NON_SELF_TYPE_BASED_CONSTRAINT
       	false HAS_NO_DEPENDENCIES_TO_OTHER_VARIABLES
       	 true HAS_PROPER_NON_TRIVIAL_CONSTRAINTS
       	false HAS_PROPER_NON_TRIVIAL_CONSTRAINTS_OTHER_THAN_INCORPORATED_FROM_DECLARED_UPPER_BOUND
       	false REIFIED
       	false HAS_PROPER_FLEXIBLE_LOWER_CONSTRAINT
       	 true HAS_PROPER_NON_ILT_CONSTRAINT
       	 true HAS_NO_EXPLICIT_LOWER_NOTHING_CONSTRAINT
       	false HAS_PROPER_EQUALITY_CONSTRAINT
       	false HAS_PROPER_NON_NOTHING_NON_UPPER_CONSTRAINT
       )`
2. `TypeVariable(T2) == kotlin/Any` _from Fix variable T2_
3. Combine `TypeVariable(T2) == TypeVariable(T1)` with `TypeVariable(T2) == kotlin/Any`
    1. `TypeVariable(T1) <: kotlin/Any`
4. Combine `TypeVariable(T2) == TypeVariable(X1)` with `TypeVariable(T2) == kotlin/Any`
    1. `TypeVariable(X1) <: kotlin/Any`
5. Combine `TypeVariable(T2) == TypeVariable(X2)` with `TypeVariable(T2) == kotlin/Any`
    1. `TypeVariable(X2) <: kotlin/Any`
6. Combine `TypeVariable(T2) == kotlin/Any` with `TypeVariable(T2) == TypeVariable(T1)`
    1. `kotlin/Any <: TypeVariable(T1)`
7. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
8. Combine `TypeVariable(T2) == kotlin/Any` with `TypeVariable(T2) == TypeVariable(X1)`
    1. `kotlin/Any <: TypeVariable(X1)`
9. Combine `TypeVariable(T2) == kotlin/Any` with `TypeVariable(T2) == TypeVariable(X2)`
    1. `kotlin/Any <: TypeVariable(X2)`
10. Combine `TypeVariable(T1) <: kotlin/Any` with `kotlin/Any <: TypeVariable(T1)`
    1. `TypeVariable(T1) == kotlin/Any`
11. Combine `TypeVariable(T1) == TypeVariable(T2)` with `TypeVariable(T1) == kotlin/Any`
    1. `TypeVariable(T2) <: kotlin/Any`
12. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
13. Combine `TypeVariable(T1) == kotlin/Any` with `TypeVariable(T1) == TypeVariable(T2)`
    1. `kotlin/Any <: TypeVariable(T2)`
14. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
15. Combine `TypeVariable(T1) == kotlin/Any` with `TypeVariable(T1) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(T1) <: ExtendedEntity<kotlin/Any>`
16. Combine `TypeVariable(T1) == kotlin/Any` with `TypeVariable(T2) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(T2) <: ExtendedEntity<kotlin/Any>`
17. Combine `TypeVariable(T1) == kotlin/Any` with `TypeVariable(X1) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(X1) <: ExtendedEntity<kotlin/Any>`
18. Combine `TypeVariable(T1) == kotlin/Any` with `TypeVariable(X2) <: ExtendedEntity<TypeVariable(T1)>`
    1. `TypeVariable(X2) <: ExtendedEntity<kotlin/Any>`
19. Combine `TypeVariable(X1) <: kotlin/Any` with `kotlin/Any <: TypeVariable(X1)`
    1. `TypeVariable(X1) == kotlin/Any`
20. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
21. Combine `TypeVariable(X2) <: kotlin/Any` with `kotlin/Any <: TypeVariable(X2)`
    1. `TypeVariable(X2) == kotlin/Any`
22. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
23. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
24. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
25. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
26. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
27. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
28. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
29. Choose `TypeVariable(T1)` with `Readiness(
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
    1. `TypeVariable(X1)` is `Readiness(
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
    2. `TypeVariable(X2)` is `Readiness(
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
30. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
31. __NewConstraintError: `kotlin/Any <: ExtendedEntity<TypeVariable(T1)>`__
32. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
33. Choose `TypeVariable(X1)` with `Readiness(
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
    1. `TypeVariable(X2)` is `Readiness(
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
34. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
35. Choose `TypeVariable(X2)` with `Readiness(
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
36. __NewConstraintError: `kotlin/Any <: ExtendedEntity<kotlin/Any>`__
